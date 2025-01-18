package com.mrmccormick.hydra.mqtt.domain.actor;

import org.jetbrains.annotations.NotNull;

public interface IRunnerBuilder {
    IRunner build(@NotNull IRunnable runnable);
}
