package com.mrmccormick.hydra.mqtt.domain.actor;

import com.mrmccormick.hydra.mqtt.domain.Event;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface IActor {
    /**
     * Add a subscriber to the publisher.
     *
     * @param subscriber The subscriber to add to the publisher.
     */
    void addSubscriber(@NotNull IActor subscriber);

    /**
     * Clear all caches and data in the actor.
     *
     * @return Whether all caches and data in the actor were successfully cleared.
     */
    boolean clear();

    /**
     * Get the name of the actor.
     *
     * @return The name of the actor.
     */
    @NotNull String getName();

    /**
     * Get all subscribers to the current publisher.
     *
     * @return All subscribers to the current publisher.
     */
    @NotNull List<IActor> getSubscribers();

    /**
     * Get whether a subscriber is subscribed to the current publisher.
     *
     * @param subscriber The subscriber to check.
     * @return Whether the subscriber is subscribed to the current publisher.
     */
    boolean isSubscriber(@NotNull IActor subscriber);

    /**
     * Receive an event.
     *
     * @param event The event being received.
     */
    void receive(@NotNull Event event);

    /**
     * Remove all subscribers from the current publisher.
     */
    void removeAllSubscribers();

    /**
     * Remove a subscriber from the current publisher.
     *
     * @param subscriber The subscriber to remove.
     */
    void removeSubscriber(@NotNull IActor subscriber);
}
