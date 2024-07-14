package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.mrmccormick.ignition.hydra.mqtt.GatewayHook;

import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.gateway.model.IgnitionWebApp;
import com.inductiveautomation.ignition.gateway.web.components.RecordEditForm;
import com.inductiveautomation.ignition.gateway.web.models.DefaultConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.IConfigTab;
import com.inductiveautomation.ignition.gateway.web.models.LenientResourceModel;
import com.inductiveautomation.ignition.gateway.web.pages.IConfigPage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Application;

public class SettingsPageX extends RecordEditForm {

    public static final String MENU_LOCATION_KEY = SettingsPageX.class.getSimpleName();

    public static final String BUNDLE_FILE_NAME = SettingsPageX.class.getSimpleName();

    public static final String BUNDLE_PREFIX = SettingsPageX.class.getSimpleName();

    public static final IConfigTab CONFIG_ENTRY = DefaultConfigTab.builder()
            .category(SettingsCategory.CONFIG_CATEGORY)
            .name(SettingsPageX.MENU_LOCATION_KEY)
            .i18n(BUNDLE_PREFIX + ".nav.settings.title")
            .page(SettingsPageX.class)
            .terms("Settings")
            .build();

    public static final Pair<String, String> MENU_LOCATION =
        Pair.of(SettingsCategory.CONFIG_CATEGORY.getName(), MENU_LOCATION_KEY);

    public SettingsPageX(final IConfigPage configPage) {
        super(configPage,
                null,
                new LenientResourceModel( BUNDLE_PREFIX + ".nav.settings.panelTitle"),
                ((IgnitionWebApp) Application.get()).getContext().getPersistenceInterface().find(SettingsRecordX.META,
                        0L)
        );
    }

    @Override
    public Pair<String, String> getMenuLocation() {
        return MENU_LOCATION;
    }

    public static void Setup(GatewayHook gatewayHook) {
        BundleUtil.get().addBundle(BUNDLE_PREFIX, gatewayHook.getClass(), BUNDLE_FILE_NAME);
    }

    public static void Shutdown() {
        BundleUtil.get().removeBundle(BUNDLE_PREFIX);
    }
}
