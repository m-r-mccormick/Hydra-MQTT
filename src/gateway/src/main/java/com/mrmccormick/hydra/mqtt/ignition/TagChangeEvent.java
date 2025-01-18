package com.mrmccormick.hydra.mqtt.ignition;

import com.inductiveautomation.ignition.common.model.values.QualifiedValue;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ClassCanBeRecord")
public class TagChangeEvent {
    public final @NotNull TagPath tagPath;
    public final @NotNull QualifiedValue value;

    public TagChangeEvent(@NotNull TagPath tagPath, @NotNull QualifiedValue value) {
        //noinspection ConstantValue
        if (tagPath == null)
            throw new IllegalArgumentException("tagPath cannot be null");
        this.tagPath = tagPath;

        //noinspection ConstantValue
        if (value == null)
            throw new IllegalArgumentException("value cannot be null");
        this.value = value;
    }
}
