package com.mrmccormick.ignition.hydra.mqtt;

import com.mrmccormick.ignition.hydra.mqtt.data.JsonCoder;
import com.mrmccormick.ignition.hydra.mqtt.settings.SettingsManager;

import java.util.*;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.*;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;


public class GatewayHook extends AbstractGatewayModuleHook {

    public static Logger GetLogger(Class c) {
        String root = "Hydra-MQTT";
        if (c == null) {
            return LogManager.getLogger(root);
        }
        var names = c.getName().split("\\.");
        var name = names[names.length - 1];
        var logger = LogManager.getLogger(root + "_" + name);
        logger.setLevel(Level.TRACE);
        return logger;
    }

    private final Logger _logger = GetLogger(null);
    private GatewayContext _context;
    private TagManager _tagManagerSubscribe;
    private MqttManager _mqttManager;
    private SettingsManager _settingsManager;

    @Override
    public void setup(GatewayContext context) {
        _logger.info("Setting up module...");

        if (context == null) {
            _logger.fatal("Module setup function received null context");
            return;
        }
        _context = context;

        try {
            _settingsManager = new SettingsManager(_context, this);
        } catch (Exception e) {
            _logger.fatal("Error loading configuration: " + e.getMessage(), e);
            return;
        }

        try {
            var subProvider = _settingsManager.SettingsRecord.getTagProviderSub();
            if (subProvider != null) {
                _tagManagerSubscribe = new TagManager(context, subProvider);
            }
        } catch (Exception e) {
            _logger.fatal("Error configuring Tag Provider: " + e.getMessage(), e);
            return;
        }

        try {
            var subscribeValuePath = _settingsManager.SettingsRecord.getRepresentationSubscribeValuePath();
            var subscribeTimestampPath = _settingsManager.SettingsRecord.getRepresentationSubscribeTimestampPath();
            var coder = new JsonCoder(subscribeValuePath, subscribeTimestampPath);

            var host = _settingsManager.SettingsRecord.getBrokerHost();
            var port = _settingsManager.SettingsRecord.getBrokerPort();
            var subQos = _settingsManager.SettingsRecord.getBrokerSubscribeQos();;
            var brokerSubscriptions = _settingsManager.SettingsRecord.getBrokerSubscriptions();

            List<String> subscriptions = new ArrayList<>(Arrays.asList(brokerSubscriptions.split("\r\n")));
            _mqttManager = new MqttManager(host, port, subQos, coder, subscriptions);
        } catch (Exception e) {
            _logger.fatal("Error configuring MQTT client: " + e.getMessage(), e);
            return;
        }

        try {
            if (_tagManagerSubscribe != null) {
                _mqttManager.DataEventSubscribers.add(_tagManagerSubscribe);
            }
        } catch (Exception e) {
            _logger.fatal("Error setting up module: " + e.getMessage(), e);
            return;
        }

        _logger.info("Set up module successfully.");
    }

    @Override
    public void startup(LicenseState activationState) {
        _logger.info("Starting module...");
        try {
            _settingsManager.Startup();

            if (_tagManagerSubscribe != null)
                _tagManagerSubscribe.Startup();

             _mqttManager.Startup(true);

            _context.getExecutionManager().register(getClass().getName(), "Maintain", this::maintain_mqtt_connection, 5000);

            _logger.info("Module started.");
        } catch (Exception e) {
            _logger.fatal("Error starting module.", e);
        }

    }

    @Override
    public void shutdown() {
        _logger.info("Stopping module...");
        try {
            if (_tagManagerSubscribe != null) {
                _tagManagerSubscribe.Shutdown();
                _tagManagerSubscribe = null;
            }
            if (_mqttManager != null) {
                _mqttManager.Shutdown();
                _mqttManager = null;
            }
            if (_settingsManager != null) {
                _settingsManager.Shutdown();
                _settingsManager = null;
            }
            if (_context != null) {
                _context.getExecutionManager().unRegister(getClass().getName(), "Maintain");
            }
            _logger.info("Stopped module.");
        } catch (Exception e) {
            _logger.error("Error stopping module.", e);
        }
    }

    private synchronized void maintain_mqtt_connection() {
        if (_mqttManager != null) {
            if (!_mqttManager.IsConnected()) {
                _mqttManager.Reconnect();
            }
        }
    }

    public boolean isFreeModule() {
        return true;
    }

    public boolean isMakerEditionCompatible() {
        return true;
    }

    @Override
    public List<ConfigCategory> getConfigCategories() {
        return SettingsManager.ConfigCategories;
    }

    @Override
    public List<? extends IConfigTab> getConfigPanels() {
        return SettingsManager.ConfigPanels;
    }
}
