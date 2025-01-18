package com.mrmccormick.hydra.mqtt.domain.actor;

import org.jetbrains.annotations.Nullable;

public interface IRunner {
    @Nullable IRunnable getActor();

    void stop();
}
