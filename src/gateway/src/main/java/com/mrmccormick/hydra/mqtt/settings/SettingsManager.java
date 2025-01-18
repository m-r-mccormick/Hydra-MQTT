package com.mrmccormick.hydra.mqtt.settings;

import com.mrmccormick.hydra.mqtt.Connection;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import com.mrmccormick.hydra.mqtt.domain.settings.IConnectionSettingsProvider;
import com.mrmccormick.hydra.mqtt.domain.settings.ISettingsManager;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

public class SettingsManager implements ISettingsManager {

    public SettingsManager(@NotNull GatewayContext context, @NotNull GatewayHook gatewayHook) throws Exception {
        //noinspection ConstantValue
        if (context == null)
            throw new NullPointerException("Gateway context can not null");

        //noinspection ConstantValue
        if (gatewayHook == null)
            throw new NullPointerException("Gateway hook can not be null");

        SettingsCategory.Setup(gatewayHook);
        // Only have to set up once for all SettingsPageConnection(s)
        MqttConnectionPage.Setup(gatewayHook);

        // Update Record Schemas
        try {
            MqttConnectionSettingsRecord.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }

        List<IConnectionSettingsProvider> providers = new ArrayList<>();

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        for (var provider : providers) {
            provider.setGatewayContext(context);
            var connection = new Connection("Connection " + _connections.size(), provider);
            provider.getChangeHandler().setConnection(connection);
            _connections.add(connection);
        }
    }

    public void close() {
        SettingsCategory.Shutdown();
        // Only have to shut down once for all MqttConnectionPage(s)
        MqttConnectionPage.Shutdown();
    }

    public static @NotNull List<ConfigCategory> getConfigCategories() {
        return _configCategories;
    }

    public static @NotNull List<IConfigTab> getConfigPanels() {
        return new ArrayList<>(_configPanels);
    }

    public @NotNull List<Connection> getConnections() {
        return _connections;
    }

    private static final @NotNull List<ConfigCategory> _configCategories = List.of(
            SettingsCategory.CONFIG_CATEGORY
    );

    private static final @NotNull List<IConfigTab> _configPanels = List.of(
            MqttConnectionPage.CONFIG_ENTRY
    );
    private final List<Connection> _connections = new ArrayList<>();
}
