package com.mrmccormick.hydra.mqtt.domain.actor.connector;

import com.mrmccormick.hydra.mqtt.domain.Event;
import org.jetbrains.annotations.NotNull;

public interface ICoder {

    @NotNull Event decode(@NotNull String path, @NotNull byte[] bytes) throws Exception;

    @NotNull byte[] encode(@NotNull Event event) throws Exception;
}
