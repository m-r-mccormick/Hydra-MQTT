package com.mrmccormick.hydra.mqtt.settings;

import com.mrmccormick.hydra.mqtt.GatewayHook;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;

public class SettingsCategory {

    // File name without .properties extension
    public static final String BUNDLE_FILE_NAME = SettingsCategory.class.getSimpleName();
    public static final String BUNDLE_PREFIX = SettingsCategory.class.getSimpleName();
    // Displayed in header navigation at top of page: () Config > CATEGORY_NAME > Panel Title
    public static final String CATEGORY_NAME = "Hydra-MQTT";
    public static final ConfigCategory CONFIG_CATEGORY =
            new ConfigCategory(CATEGORY_NAME, BUNDLE_PREFIX + ".nav.header", 700);

    public static void Setup(GatewayHook gatewayHook) {
        // Register GatewayHook.properties by registering the GatewayHook.class with BundleUtils
        BundleUtil.get().addBundle(BUNDLE_PREFIX, gatewayHook.getClass(), BUNDLE_FILE_NAME);
    }

    public static void Shutdown() {
        BundleUtil.get().removeBundle(BUNDLE_PREFIX);
    }
}
