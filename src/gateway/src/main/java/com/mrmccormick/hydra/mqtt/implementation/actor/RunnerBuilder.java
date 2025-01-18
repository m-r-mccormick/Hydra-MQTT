package com.mrmccormick.hydra.mqtt.implementation.actor;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunnable;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunner;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunnerBuilder;
import org.jetbrains.annotations.NotNull;

public class RunnerBuilder implements IRunnerBuilder {
    @Override
    public IRunner build(@NotNull IRunnable runnable) {
        return new Runner(_gatewayContext, runnable);
    }

    public RunnerBuilder(@NotNull GatewayContext gatewayContext) {
        //noinspection ConstantValue
        if (gatewayContext == null)
            throw new IllegalArgumentException("GatewayContext can not be null");
        _gatewayContext = gatewayContext;
    }

    private final GatewayContext _gatewayContext;

}
