package com.mrmccormick.hydra.mqtt.domain.actor;

public interface IRunnable {
    /**
     * Get the runner which is running the actor.
     *
     * @return The runner which is running the actor.
     */
    IRunner getRunner();

    /**
     * Run the runnable actor.
     */
    void run();
}
