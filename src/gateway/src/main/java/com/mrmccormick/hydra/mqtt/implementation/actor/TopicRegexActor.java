package com.mrmccormick.hydra.mqtt.implementation.actor;

import com.mrmccormick.hydra.mqtt.domain.Event;
import com.mrmccormick.hydra.mqtt.domain.actor.IActor;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunner;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunnable;
import com.mrmccormick.hydra.mqtt.domain.actor.IRunnerBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TopicRegexActor implements IActor, IRunnable {
    public TopicRegexActor(@NotNull String connectionName,
                           @NotNull String name,
                           @NotNull List<String> topicIncludeRegex,
                           @NotNull List<String> topicExcludeRegex,
                           @NotNull IRunnerBuilder runnerBuilder) {
        //noinspection ConstantValue
        if (connectionName == null)
            throw new IllegalArgumentException("connectionName can not be null");

        //noinspection ConstantValue
        if (name == null)
            throw new IllegalArgumentException("name can not be null");
        _name = name;

        //noinspection ConstantValue
        if (topicIncludeRegex == null)
            throw new IllegalArgumentException("topicIncludeRegex can not be null");
        _topicIncludeRegex = new ArrayList<>();
        for (var patternString : topicIncludeRegex)
            _topicIncludeRegex.add(Pattern.compile(patternString));

        //noinspection ConstantValue
        if (topicExcludeRegex == null)
            throw new IllegalArgumentException("topicExcludeRegex can not be null");
        _topicExcludeRegex = new ArrayList<>();
        for (var patternString : topicExcludeRegex)
            _topicExcludeRegex.add(Pattern.compile(patternString));

        //noinspection ConstantValue
        if (runnerBuilder == null)
            throw new IllegalArgumentException("runnerBuilder can not be null");
        _runner = runnerBuilder.build(this);
    }

    @Override
    public void addSubscriber(@NotNull IActor subscriber) {
        _subscribers.add(subscriber);
    }

    @Override
    public boolean clear() {
        return true;
    }

    @Override
    public boolean isSubscriber(@NotNull IActor subscriber) {
        return _subscribers.contains(subscriber);
    }

    @Override
    public @NotNull String getName() {
        return _name;
    }

    @Override
    public IRunner getRunner() {
        return _runner;
    }

    @Override
    public @NotNull List<IActor> getSubscribers() {
        return new ArrayList<>(_subscribers);
    }

    @Override
    public void receive(@NotNull Event event) {
        boolean matchFound = false;
        for (var pattern : _topicIncludeRegex) {
            Matcher matcher = pattern.matcher(event.path);
            if (matcher.matches()) {
                matchFound = true;
                break;
            }
        }

        if (!matchFound)
            return;

        for (var pattern : _topicExcludeRegex) {
            Matcher matcher = pattern.matcher(event.path);
            if (matcher.matches())
                return;
        }

        for (var subscriber : _subscribers) {
            subscriber.receive(event);
        }
    }

    @Override
    public void removeAllSubscribers() {
        _subscribers.clear();
    }

    @Override
    public void removeSubscriber(@NotNull IActor subscriber) {
        _subscribers.remove(subscriber);
    }

    @Override
    public void run() {

    }

    private final @NotNull String _name;
    private final IRunner _runner;
    private final @NotNull List<IActor> _subscribers = new ArrayList<>();
    private final @NotNull List<Pattern> _topicExcludeRegex;
    private final @NotNull List<Pattern> _topicIncludeRegex;
}
