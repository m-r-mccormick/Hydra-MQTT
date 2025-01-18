package com.mrmccormick.hydra.mqtt;

import com.mrmccormick.hydra.mqtt.ignition.TagChangeActorFactory;
import com.mrmccormick.hydra.mqtt.domain.settings.ISettingsManager;
import com.mrmccormick.hydra.mqtt.settings.SettingsManager;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.*;

import java.util.*;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GatewayHook extends AbstractGatewayModuleHook {

    public static final String moduleId = "com.mrmccormick.hydra.mqtt";

    public boolean connect() {
        _logger.debug("connect starting...");
        if (_context == null) {
            _logger.fatal("Module was not provided gateway context, can not start.");
            return false;
        }

        try {
            _settingsProvider = new SettingsManager(_context, this);
        } catch (Exception e) {
            _logger.fatal("Error loading configuration: " + e.getMessage(), e);
            return false;
        }

        try {
            _connections.addAll(_settingsProvider.getConnections());
        } catch (Exception e) {
            _logger.fatal("Error getting enabled connections: " + e.getMessage(), e);
            return false;
        }

        var error = false;
        for (var connection : _connections) {
            try {
                connection.reconnectWithNewSettingsIfChanged();
            } catch (Exception e) {
                _logger.warn("Could not establish " + connection.name + ": " + e, e);
                error = true;
            }
        }

        try {
            var reconnectMinutes = 1;
            _context.getExecutionManager().register(getClass().getName(), "Maintain", this::maintainConnections,
                    reconnectMinutes * 60000);
        } catch (Exception e) {
            _logger.fatal("Error registering Maintain task: " + e.getMessage(), e);
            error = true;
        }

        _logger.debug("connect ended, error: " + error);
        return !error;
    }

    public void disconnect() {
        _logger.debug("disconnect starting...");

        if (_context == null) {
            _logger.fatal("Module was not provided gateway context, can not disconnect.");
            return;
        }
        _context.getExecutionManager().unRegisterAll(getClass().getName());

        for (var connection : _connections) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                _logger.warn("Error disconnecting " + connection.name + ": " + e.getMessage(), e);
            }
        }
        _connections.clear();

        _logger.debug("disconnect ended.");
    }

    @Override
    public List<ConfigCategory> getConfigCategories() {
        if (_settingsProvider != null)
            return SettingsManager.getConfigCategories();
        return new ArrayList<>();
    }

    @Override
    public List<? extends IConfigTab> getConfigPanels() {
        if (_settingsProvider != null)
            return SettingsManager.getConfigPanels();
        return new ArrayList<>();
    }

    public static Logger getLogger(@NotNull Class c) {
        //noinspection ConstantValue
        if (c == null)
            throw new NullPointerException("Class cannot be null");

        var className = c.getSimpleName();
        Logger logger = LogManager.getLogger(_loggerRootName + "." + className);
        logger.setLevel(Level.TRACE);
        return logger;
    }

    public static Logger getLogger(@NotNull String name) {
        //noinspection ConstantValue
        if (name == null)
            throw new NullPointerException("name cannot be null");

        var logger = LogManager.getLogger(_loggerRootName + "." + name.replace(" ", ""));
        logger.setLevel(Level.TRACE);
        return logger;
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }

    @Override
    public boolean isMakerEditionCompatible() {
        return true;
    }

    @Override
    public void notifyLicenseStateChanged(LicenseState licenseState) {
        try {
            if (licenseState.isTrialExpired()) {
                for (var connection : _connections)
                    connection.setEnabled(false);
            } else {
                for (var connection : _connections)
                    connection.setEnabled(true);
            }
        } catch (Exception e) {
            _logger.error("Error setting connection enabled after license state change: " + e, e);
        }
    }

    @Override
    public void setup(GatewayContext context) {
        _logger.info("Initializing module...");
        var startTimeMs = System.currentTimeMillis();

        if (context == null) {
            _logger.fatal("Module setup function received null gateway context");
            return;
        }

        TagChangeActorFactory.register(context.getTagManager().getConfigManager());

        _context = context;
        String duration = String.format("%.3f", ((double) (System.currentTimeMillis() - startTimeMs)) / 1000);
        _logger.info("Initialized module successfully in " + duration + " seconds.");
    }

    @Override
    public void shutdown() {
        _logger.info("Stopping module...");
        var startTimeMs = System.currentTimeMillis();

        disconnect();

        TagChangeActorFactory.unregister(_context.getTagManager().getConfigManager());

        String duration = String.format("%.3f", ((double) (System.currentTimeMillis() - startTimeMs)) / 1000);
        _logger.info("Stopped module in " + duration + " seconds.");
    }

    @Override
    public void startup(LicenseState activationState) {
        _logger.info("Starting module...");
        var startTimeMs = System.currentTimeMillis();

        String duration = String.format("%.3f", ((double) (System.currentTimeMillis() - startTimeMs)) / 1000);
        if (!connect()) {
            _logger.fatal("Failed to establish connections in " + duration + " seconds.");
            return;
        }

        _logger.info("Module started in " + duration + " seconds.");
    }

    private final @NotNull List<Connection> _connections = new ArrayList<>();
    private GatewayContext _context;
    private final @NotNull Logger _logger = getLogger();
    private static final @NotNull String _loggerRootName = "Hydra-MQTT";
    private ISettingsManager _settingsProvider;

    private static Logger getLogger() {
        return LogManager.getLogger(_loggerRootName);
    }

    private void maintainConnections() {
        try {
            for (var connection : _connections)
                connection.maintain();
        } catch (Exception e) {
            _logger.error("Exception maintaining connections:" + e, e);
        }
    }
}
