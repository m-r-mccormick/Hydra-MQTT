package com.mrmccormick.hydra.mqtt;

import com.mrmccormick.hydra.mqtt.domain.actor.IActor;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunnable;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.IConnector;
import com.mrmccormick.hydra.mqtt.domain.settings.IConnectionSettingsProvider;
import com.mrmccormick.hydra.mqtt.settings.ConnectionSettings;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Connection {

    public @Nullable IConnector externalConnector;
    public @NotNull
    final String name;

    public Connection(@NotNull String name, @NotNull IConnectionSettingsProvider connectionSettingsProvider) {
        //noinspection ConstantValue
        if (name == null)
            throw new IllegalArgumentException("name can not be null");
        this.name = name;
        _logger = GatewayHook.getLogger(name);

        //noinspection ConstantValue
        if (connectionSettingsProvider == null)
            throw new IllegalArgumentException("connectionSettingsProvider can not be null");
        _connectionSettingsProvider = connectionSettingsProvider;
    }

    public boolean clear() {
        var ret = true;
        if (_actors != null)
            for (var actor : _actors)
                if (!actor.clear())
                    ret = false;
        return ret;
    }

    public void connect() throws Exception {
        _logger.debug("Attempting to connect all connectors...");

        for (var connector : getConnectors())
            connector.connect();

        _logger.debug("Attempted to connect all connectors.");
    }

    public void disconnect() throws Exception {
        _logger.debug("Attempting to disconnect all connectors...");

        for (IRunnable actor : getRunnableActors())
            actor.getRunner().stop();

        for (var connector : getConnectors()) {
            if (connector.isConnecting()) {
                _logger.debug("Connector " + connector.getName() + " is connecting, waiting for it to abort.");
                var connected = false;
                try {
                    for (int i = 0; i < 100; i++) {
                        connector.abortConnecting();
                        Thread.sleep(100);
                        if (!connector.isConnecting()) {
                            connected = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    _logger.debug("Exception waiting for abort connect: " + e, e);
                }
                if (connected)
                    _logger.debug("Connector " + connector.getName() + " connect aborted.");
                else
                    _logger.warn("Timeout exceeded waiting for " + connector.getName() + " to abort connect.");
            }

            if (connector.isConnected() && !connector.isDisconnecting()) {
                _logger.debug("Connector " + connector.getName() + " is not connected and not disconnecting. " +
                        "Executing disconnect.");
                connector.disconnect();
            }

            if (connector.isDisconnecting()) {
                _logger.debug("Connector " + connector.getName() + " is disconnecting, waiting for it to finish.");
                var disconnected = false;
                try {
                    for (int i = 0; i < 100; i++) {
                        Thread.sleep(100);
                        if (!connector.isDisconnecting()) {
                            disconnected = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    _logger.debug("Exception waiting for disconnect: " + e, e);
                }
                if (disconnected)
                    _logger.debug("Connector " + connector.getName() + " disconnected.");
                else
                    _logger.warn("Timeout exceeded waiting for " + connector.getName() + " to disconnect.");
            }
        }
        _logger.debug("Attempted to disconnect all connectors.");
    }

    public @NotNull List<IActor> getActors() {
        if (_actors == null)
            return new ArrayList<>();
        return new ArrayList<>(_actors);
    }

    public @NotNull List<IConnector> getConnectors() {
        var connectors = new ArrayList<IConnector>();
        for (var actor : getActors())
            if (actor instanceof IConnector)
                connectors.add((IConnector) actor);
        return connectors;
    }

    public @NotNull List<IRunnable> getRunnableActors() {
        var runnableActors = new ArrayList<IRunnable>();
        for (var actor : getActors())
            if (actor instanceof IRunnable)
                runnableActors.add((IRunnable) actor);
        return runnableActors;
    }

    public void maintain() {
        _logger.debug("Attempting to maintain the connection of all connectors...");

        for (var connector : getConnectors()) {
            if (connector.isConnected()) {
                _logger.debug(connector.getName() + " is connected.");
                continue;
            }

            if (connector.isConnecting()) {
                _logger.debug(connector.getName() + " is connecting.");
                continue;
            }

            _logger.info(connector.getName() + " is not connected. Attempting to connect/reconnect...");
            try {
                connector.connect();
            } catch (Exception e) {
                _logger.error("Exception attempting to connect/reconnect " + connector.getName() + ": " + e, e);
            }
        }

        _logger.debug("Attempted to maintain the connection of all connectors.");
    }

    public boolean reconnectWithNewSettingsIfChanged() {
        if (_connectionSettingsProvider == null)
            throw new IllegalStateException("connectionSettingsProvider has not been initialized");
        var newProvider = _connectionSettingsProvider.reload();
        var newSettings = newProvider.getSettings();

        if (newSettings.connectionEnabled == null) {
            _logger.error("Connection enabled returned null");
            return false;
        }

        List<IActor> newActors = null;
        if (newSettings.connectionEnabled) {
            List<String> errors = newSettings.validate();
            if (!errors.isEmpty()) {
                for (var error : errors)
                    _logger.error(error);
                return false;
            }

            try {
                newActors = newSettings.load(name);
            } catch (Exception e) {
                _logger.error("Failed to load settings: " + e, e);
                return false;
            }
        }

        // Only disconnect and use new settings if loading new settings was successful.
        //  Otherwise, allow to continue with previous configuration until the user updates
        //  the settings to be valid.
        if (_connectionSettings != null) {
            try {
                disconnect();
            } catch (Exception e) {
                _logger.warn("Failed to disconnect: " + e, e);
            }
        }

        if (newSettings.connectionEnabled) {
            if (newActors != null)
                _actors = newActors;
            externalConnector = newSettings.getExternalConnector();
        }
        _connectionSettingsProvider = newProvider;
        _connectionSettings = newSettings;

        if (!newSettings.connectionEnabled)
            return true; // Connection is not enabled, so don't try to connect.

        try {
            connect();
        } catch (Exception e) {
            _logger.error("Failed to connect: " + e, e);
            return false;
        }

        return true;
    }

    public void setEnabled(boolean enabled) {
        _logger.debug("Setting enabled = " + enabled + " on all connectors...");
        for (var connector : getConnectors())
            connector.setEnabled(enabled);
        _logger.debug("Set enabled = " + enabled + " on all connectors.");
    }

    private @Nullable List<IActor> _actors;
    private @Nullable ConnectionSettings _connectionSettings = null;
    private @Nullable IConnectionSettingsProvider _connectionSettingsProvider;
    private final @NotNull Logger _logger;
}