package com.mrmccormick.hydra.mqtt.domain.settings;

import com.mrmccormick.hydra.mqtt.Connection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ISettingsManager {
    @NotNull List<Connection> getConnections() throws Exception;
}
