package com.mrmccormick.hydra.mqtt.implementation.actor;

import com.inductiveautomation.ignition.common.execution.SchedulingController;
import com.inductiveautomation.ignition.common.execution.SelfSchedulingRunnable;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunner;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Runner implements SelfSchedulingRunnable, IRunner {
    public long nextExecDelayMillis = 1;

    public Runner(@NotNull GatewayContext gatewayContext, @NotNull IRunnable runnable) {
        //noinspection ConstantValue
        if (gatewayContext == null)
            throw new IllegalArgumentException("GatewayContext can not be null");
        this._gatewayContext = gatewayContext;

        //noinspection ConstantValue
        if (runnable == null)
            throw new IllegalArgumentException("Runnable can not be null");
        _runnable = runnable;

        _name = String.valueOf(++_instance);
        _gatewayContext.getExecutionManager().register(getClass().getName(), _name, this);
    }

    public @Nullable IRunnable getActor() {
        return _runnable;
    }

    @Override
    public long getNextExecDelayMillis() {
        // If set to 0, disables? Then have to _schedulingController.requestReschedule(this)
        return nextExecDelayMillis;
    }

    @Override
    public void run() {
        if (_runnable != null)
            _runnable.run();
    }

    @Override
    public void setController(@Nullable SchedulingController schedulingController) {
    }

    @Override
    public void stop() {
        nextExecDelayMillis = 0;
        _gatewayContext.getExecutionManager().unRegister(getClass().getName(), _name);
        _runnable = null;
    }

    private final GatewayContext _gatewayContext;
    private static int _instance = 0;
    private final String _name;
    private @Nullable IRunnable _runnable;
}
