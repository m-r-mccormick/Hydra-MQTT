package com.mrmccormick.hydra.mqtt.domain.actor;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ActorBase implements IActor {
    public ActorBase(@NotNull String name) {
        //noinspection ConstantValue
        if (name == null)
            throw new NullPointerException("name can not be null");
        _name = name;
    }

    @Override
    public void addSubscriber(@NotNull IActor subscriber) {
        //noinspection ConstantValue
        if (subscriber == null)
            throw new NullPointerException("subscriber cannot be null");

        _subscribers.add(subscriber);
    }

    public @NotNull String getName() {
        return _name;
    }

    @Override
    public @NotNull List<IActor> getSubscribers() {
        return new ArrayList<>(_subscribers);
    }

    @Override
    public boolean isSubscriber(@NotNull IActor subscriber) {
        //noinspection ConstantValue
        if (subscriber == null)
            throw new NullPointerException("subscriber cannot be null");

        return _subscribers.contains(subscriber);
    }

    @Override
    public void removeAllSubscribers() {
        _subscribers.clear();
    }

    @Override
    public void removeSubscriber(@NotNull IActor subscriber) {
        //noinspection ConstantValue
        if (subscriber == null)
            throw new NullPointerException("subscriber cannot be null");

        _subscribers.remove(subscriber);
    }

    private final String _name;
    protected final @NotNull List<IActor> _subscribers = new ArrayList<>();
}
