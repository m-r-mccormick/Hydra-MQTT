package com.mrmccormick.ignition.hydra.mqtt;

import com.mrmccormick.ignition.hydra.mqtt.settings.SettingsManager;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
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
    private SettingsManager _settingsManager;
    private final List<Connection> _connections = new ArrayList<>();

    @Override
    public void setup(GatewayContext context) {

        _logger.info("Configuring module...");
        var startTimeMs = System.currentTimeMillis();

        if (context == null) {
            _logger.fatal("Module setup function received null gateway context");
            return;
        }

        try {
            _settingsManager = new SettingsManager(context, this);
        } catch (Exception e) {
            _logger.fatal("Error loading configuration: " + e.getMessage(), e);
            return;
        }

        _context = context;
        String duration = String.format("%.3f", ((double)(System.currentTimeMillis() - startTimeMs)) / 1000);
        _logger.info("Configured module successfully in " + duration + " seconds.");
    }

    @Override
    public void startup(LicenseState activationState) {
        if (_context == null) {
            _logger.warn("Module was not successfully configured, can not start.");
            return;
        }

        _logger.info("Starting module...");
        var startTimeMs = System.currentTimeMillis();

        try {
            _settingsManager.Startup();
        } catch (Exception e) {
            _logger.fatal("Error starting settings manager: " + e.getMessage(), e);
            try {
                _settingsManager.Shutdown();
            } catch (Exception e2) {
                _logger.error("Error stopping settings manager after failed start: " + e2.getMessage(), e);
            }
            return;
        }

        List<Connection> enabledConnections;
        try {
            enabledConnections = _settingsManager.getEnabledConnections(_context);
        } catch (Exception e) {
            _logger.fatal("Error getting enabled connections: " + e.getMessage(), e);
            return;
        }

        for (var connection : enabledConnections) {
            try {
                connection.Start(_context);
                _connections.add(connection);
            } catch (Exception e) {
                _logger.warn("Could not establish " + connection.name + ": " + e.getMessage(), e);
            }
        }
        for (var connection : _connections) {
            enabledConnections.remove(connection);
        }
        var connectionFailed = !enabledConnections.isEmpty();

        boolean registerMaintainTaskFailed = false;
        try {
            _context.getExecutionManager().register(getClass().getName(), "Maintain", this::maintain_mqtt_connection, 5000);
        } catch (Exception e) {
            _logger.fatal("Error registering Maintain task: " + e.getMessage(), e);
            registerMaintainTaskFailed = true;
        }

        if (connectionFailed || registerMaintainTaskFailed) {
            if (!enabledConnections.isEmpty()) {
                _logger.fatal("Failed to establish a connection.");
                for (var connection : _connections) {
                    try {
                        connection.Start(_context);
                        _connections.add(connection);
                    } catch (Exception e) {
                        _logger.warn("Could not disconnect " + connection.name +
                                " after failed connect: "+ e.getMessage(), e);
                    }
                }
                _connections.clear();
                return;
            }
            if (!registerMaintainTaskFailed)
            {
                try {
                    _context.getExecutionManager().unRegister(getClass().getName(), "Maintain");
                } catch (Exception e) {
                    _logger.warn("Error unregistering Maintain task: " + e.getMessage(), e);
                }
            }
            return;
        }

        String duration = String.format("%.3f", ((double)(System.currentTimeMillis() - startTimeMs)) / 1000);
        _logger.info("Module started in " + duration + " seconds.");
    }

    @Override
    public void shutdown() {
        _logger.info("Stopping module...");
        var startTimeMs = System.currentTimeMillis();

        try {
            _context.getExecutionManager().unRegister(getClass().getName(), "TagBatchProcessor");
        } catch (Exception e) {
            _logger.warn("Error unregistering TagBatchProcessor task: " + e.getMessage(), e);
        }

        try {
            _context.getExecutionManager().unRegister(getClass().getName(), "Maintain");
        } catch (Exception e) {
            _logger.warn("Error unregistering Maintain task: " + e.getMessage(), e);
        }

        for (var connection : _connections) {
            try {
                connection.Stop();
            } catch (Exception e) {
                _logger.warn("Error disconnecting " + connection.name + ": "+ e.getMessage(), e);
            }
        }
        _connections.clear();

        String duration = String.format("%.3f", ((double)(System.currentTimeMillis() - startTimeMs)) / 1000);
        _logger.info("Stopped module in " + duration + " seconds.");
    }

    private synchronized void maintain_mqtt_connection() {
        for (var connection : _connections){
            connection.maintain();
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
