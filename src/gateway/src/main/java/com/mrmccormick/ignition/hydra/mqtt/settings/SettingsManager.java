package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.mrmccormick.ignition.hydra.mqtt.GatewayHook;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;

import java.sql.SQLException;
import java.util.List;

public class SettingsManager {

    public static final List<ConfigCategory> ConfigCategories = List.of(
            SettingsCategory.CONFIG_CATEGORY
    );

    public static final List<IConfigTab> ConfigPanels = List.of(
            SettingsPageX.CONFIG_ENTRY
    );

    public final SettingsRecordX SettingsRecord;

    public SettingsManager(GatewayContext context, GatewayHook gatewayHook) throws Exception {
        SettingsCategory.Setup(gatewayHook);
        SettingsPageX.Setup(gatewayHook);

        // Update Record Schemas
        try {
            SettingsRecordX.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }

        // Try to create settings
        try {
            SettingsRecordX settingsRecord = context.getLocalPersistenceInterface().createNew(SettingsRecordX.META);
            settingsRecord.setId(0L);
            SettingsRecordX.SetDefaults(settingsRecord);

            // If setting values don't already exist, set them. (Doesn't overwrite existing values)
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        // Get settings
        SettingsRecord = context.getLocalPersistenceInterface().find(SettingsRecordX.META, 0L);
        SettingsRecord.Validate();
    }

    public void Startup() {

    }

    public void Shutdown() {
        SettingsCategory.Shutdown();
        SettingsPageX.Shutdown();
    }
}
