package com.mrmccormick.hydra.mqtt.domain.settings;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.mrmccormick.hydra.mqtt.settings.ConnectionSettings;
import com.mrmccormick.hydra.mqtt.settings.ConnectionSettingsChangeHandler;
import org.jetbrains.annotations.NotNull;

public interface IConnectionSettingsProvider {
    @NotNull ConnectionSettingsChangeHandler getChangeHandler();

    @NotNull ConnectionSettings getSettings();

    @NotNull IConnectionSettingsProvider reload();

    void setGatewayContext(@NotNull GatewayContext gatewayContext);
}
