package com.mrmccormick.hydra.mqtt.settings;

import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord;
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordListenerAdapter;
import com.mrmccormick.hydra.mqtt.Connection;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConnectionSettingsChangeHandler<T extends PersistentRecord> extends RecordListenerAdapter<T> {
    public void recordUpdated(T record) {
        if (_connection == null) {
            _logger.warn("Target connection has not been set");
            return;
        }
        _logger.info("Restarting " + _connection.name + " with updated settings...");
        var startTimeMs = System.currentTimeMillis();
        boolean success;
        try {
            success = _connection.reconnectWithNewSettingsIfChanged();
        } catch (Exception e) {
            _logger.error("Failed to restart " + _connection.name + " with updated settings: " + e.getMessage(), e);
            return;
        }
        String duration = String.format("%.3f", ((double) (System.currentTimeMillis() - startTimeMs)) / 1000);
        if (success)
            _logger.info("Restarted " + _connection.name + " with updated settings in " + duration + " seconds.");
        else
            _logger.error("Failed to restart " + _connection.name + " connection with updated settings in " + duration + " seconds.");
    }

    public void setConnection(@NotNull Connection connection) {
        //noinspection ConstantValue
        if (connection == null)
            throw new IllegalArgumentException("connection can not be null");
        _connection = connection;
    }

    private @Nullable Connection _connection;
    private final Logger _logger = GatewayHook.getLogger(getClass());
}
