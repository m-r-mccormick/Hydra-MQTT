package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.mrmccormick.ignition.hydra.mqtt.Connection;
import com.mrmccormick.ignition.hydra.mqtt.GatewayHook;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class SettingsManager {

    private final Logger _logger = GatewayHook.GetLogger(getClass());

    public static final List<ConfigCategory> ConfigCategories = List.of(
            SettingsCategory.CONFIG_CATEGORY
    );

    public static final List<IConfigTab> ConfigPanels = List.of(
            SettingsPageX.CONFIG_ENTRY
    );

    public final List<IConnectSettings> SettingsBrokers = new ArrayList<>();

    public SettingsManager(GatewayContext context, GatewayHook gatewayHook) throws Exception {
        SettingsCategory.Setup(gatewayHook);
        SettingsPageX.Setup(gatewayHook);

        try {
            SettingsRecordX.UpdateSchema(context);
        } catch (SQLException e) {
            throw new Exception("Error updating configuration schema.", e);
        }

        try {
            var settingsRecord = context.getLocalPersistenceInterface().createNew(SettingsRecordX.META);
            settingsRecord.setId(0L);
            SettingsRecordX.SetDefaults(settingsRecord);
            context.getSchemaUpdater().ensureRecordExists(settingsRecord);
        } catch (Exception e) {
            throw new Exception("Error initializing configuration.", e);
        }

        SettingsBrokers.add(context.getLocalPersistenceInterface().find(SettingsRecordX.META, 0L));
    }

    public List<Connection> getEnabledConnections(GatewayContext context) throws Exception {
        List<Connection> enabledConnections = new ArrayList<>();

        var getFailed = false;
        for (var setting : SettingsBrokers) {
            if (setting.getEnabled())
            {
                try {
                    enabledConnections.add(setting.getConnection(context));
                } catch (Exception e) {
                    getFailed = true;
                    _logger.error(e.getMessage());
                }
            }
        }
        if (getFailed)
            throw new Exception("At least one Connection failed to configure.");

        return enabledConnections;
    }

    public void Startup() {

    }

    public void Shutdown() {
        SettingsCategory.Shutdown();
        SettingsPageX.Shutdown();
    }
}
