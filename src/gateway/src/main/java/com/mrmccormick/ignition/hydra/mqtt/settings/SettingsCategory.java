package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.mrmccormick.ignition.hydra.mqtt.GatewayHook;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;

public class SettingsCategory {

    public static final String CATEGORY_NAME = "Hydra-MQTT";

    public static final String BUNDLE_FILE_NAME = SettingsCategory.class.getSimpleName();

    public static final String BUNDLE_PREFIX = SettingsCategory.class.getSimpleName();

    public static final ConfigCategory CONFIG_CATEGORY =
            new ConfigCategory(CATEGORY_NAME, BUNDLE_PREFIX + ".nav.header", 700);

    public static void Setup(GatewayHook gatewayHook) {
        BundleUtil.get().addBundle(BUNDLE_PREFIX, gatewayHook.getClass(), BUNDLE_FILE_NAME);
    }

    public static void Shutdown() {
        BundleUtil.get().removeBundle(BUNDLE_PREFIX);
    }
}
