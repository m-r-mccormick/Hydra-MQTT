package com.mrmccormick.hydra.mqtt.settings;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;

public class MqttConnectionPage extends RecordEditForm {

    // File name without .properties extension
    public static final String BUNDLE_FILE_NAME = "MqttConPage"; // SettingsPageBroker.class.getSimpleName();
    public static final String BUNDLE_PREFIX = "MqttConPage"; // SettingsPageBroker.class.getSimpleName();
    public static final String MENU_LOCATION_KEY = MqttConnectionPage.class.getSimpleName();
    public static final Pair<String, String> MENU_LOCATION =
            Pair.of(SettingsCategory.CONFIG_CATEGORY.getName(), MENU_LOCATION_KEY);
    public static final int ThisId = 0;
    public static final IConfigTab CONFIG_ENTRY = DefaultConfigTab.builder()
            .category(SettingsCategory.CONFIG_CATEGORY)
            .name(MqttConnectionPage.MENU_LOCATION_KEY)
            .i18n(BUNDLE_PREFIX + ".nav.settings.title" + ThisId)
            .page(MqttConnectionPage.class)
            .terms("Settings")
            .build();

    public MqttConnectionPage(final IConfigPage configPage) {
        super(configPage,
                null,
                new LenientResourceModel(BUNDLE_PREFIX + ".nav.settings.panelTitle" + ThisId),
                ((IgnitionWebApp) Application.get()).getContext().getPersistenceInterface().find(MqttConnectionSettingsRecord.META,
                        0L)
        );
    }

    public static void Setup(GatewayHook gatewayHook) {
        // Register GatewayHook.properties by registering the GatewayHook.class with BundleUtils
        BundleUtil.get().addBundle(BUNDLE_PREFIX, gatewayHook.getClass(), BUNDLE_FILE_NAME);
    }

    public static void Shutdown() {
        BundleUtil.get().removeBundle(BUNDLE_PREFIX);
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }
}
