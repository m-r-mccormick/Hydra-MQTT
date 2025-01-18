package com.mrmccormick.hydra.mqtt.ignition;

import com.inductiveautomation.ignition.common.config.PropertySet;
import com.inductiveautomation.ignition.common.config.VersionedPropertySet;
import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.gateway.tags.evaluation.ActorClassification;
import com.inductiveautomation.ignition.gateway.tags.evaluation.ShutdownReason;
import com.inductiveautomation.ignition.gateway.tags.evaluation.TagActor;
import com.inductiveautomation.ignition.gateway.tags.evaluation.nodes.NodeContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

public class TagChangeActor implements TagActor {
    public TagChangeActor(@NotNull HashMap<String, List<ITagChangeSubscriber>> subscribers) {
        //noinspection ConstantValue
        if (subscribers == null)
            throw new IllegalArgumentException("subscribers cannot be null");
        _subscribers = subscribers;
    }

    @Override
    public ActorClassification actorClassification() {
        return TagChangeActorFactory.classification;
    }

    @Override
    public boolean attemptConfiguration(NodeContext nodeContext, VersionedPropertySet versionedPropertySet) {
        return false;
    }

    @Override
    public void destroy(NodeContext nodeContext, ShutdownReason shutdownReason) {
    }

    @Override
    public void initialize(NodeContext nodeContext, PropertySet propertySet) {
        _tagPath = nodeContext.getPath();
        var path = _tagPath.toStringFull();
        int start = path.indexOf('[') + 1;
        int end = path.indexOf(']');
        _tagProviderName = path.substring(start, end);
    }

    @Override
    public boolean onPathChanged(TagPath newPath) {
        if (_subscribers.containsKey(_tagProviderName))
            for (var subscriber : _subscribers.get(_tagProviderName))
                subscriber.onTagPathChange(_tagPath, newPath);

        _tagPath = newPath;
        // If return true, manage the path change
        // If return false, the system will rebuild the actor
        return false;
    }

    @Override
    public QualifiedValue processValue(QualifiedValue qualifiedValue) {
        // This occurs when
        //  1) The user creates a new tag in the Designer but does not assign a value, so the value is null
        //  2) A new tag config is saved (without writing value property with value in config)
        if (qualifiedValue.getValue() == null)
            return qualifiedValue;

        // If a config with value property set is saved, it will fire this method twice, once
        //      for the configuration change, and once for the value change. To avoid triggering
        //      twice for the same qualified value, check whether it is the same as the last value.
        if (_lastQualifiedValue != null)
            if (qualifiedValue.getValue() == _lastQualifiedValue.getValue() &&
                    qualifiedValue.getQuality() == _lastQualifiedValue.getQuality() &&
                    qualifiedValue.getTimestamp() == _lastQualifiedValue.getTimestamp())
                return qualifiedValue;
        _lastQualifiedValue = qualifiedValue;

        if (!_subscribers.containsKey(_tagProviderName))
            return qualifiedValue;

        for (var subscriber : _subscribers.get(_tagProviderName))
            subscriber.onTagValueChange(new TagChangeEvent(_tagPath, qualifiedValue));

        return qualifiedValue;
    }

    private QualifiedValue _lastQualifiedValue = null;
    private final HashMap<String, List<ITagChangeSubscriber>> _subscribers;
    private TagPath _tagPath;
    private String _tagProviderName;
}
