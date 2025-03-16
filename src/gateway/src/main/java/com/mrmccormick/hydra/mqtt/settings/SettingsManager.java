package com.mrmccormick.hydra.mqtt.settings;

import com.mrmccormick.hydra.mqtt.Connection;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import com.mrmccormick.hydra.mqtt.domain.settings.IConnectionSettingsProvider;
import com.mrmccormick.hydra.mqtt.domain.settings.ISettingsManager;
import com.mrmccormick.hydra.mqtt.settings.generated.*;
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
        try {
            MqttConnectionSettingsRecord1.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord2.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord3.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord4.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord5.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord6.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord7.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord8.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }
        try {
            MqttConnectionSettingsRecord9.UpdateSchema(context);
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

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord1.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord2.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord3.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord4.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord5.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord6.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord7.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord8.META);
            settingsRecord.setId(0L);
            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
            providers.add(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(MqttConnectionSettingsRecord9.META);
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
            MqttConnectionPage.CONFIG_ENTRY,
            MqttConnectionPage1.CONFIG_ENTRY,
            MqttConnectionPage2.CONFIG_ENTRY,
            MqttConnectionPage3.CONFIG_ENTRY,
            MqttConnectionPage4.CONFIG_ENTRY,
            MqttConnectionPage5.CONFIG_ENTRY,
            MqttConnectionPage6.CONFIG_ENTRY,
            MqttConnectionPage7.CONFIG_ENTRY,
            MqttConnectionPage8.CONFIG_ENTRY,
            MqttConnectionPage9.CONFIG_ENTRY
    );
    private final List<Connection> _connections = new ArrayList<>();
}
