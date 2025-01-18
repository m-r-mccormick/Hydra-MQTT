package com.mrmccormick.hydra.mqtt.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Event {

    public final @NotNull String path;
    public final @NotNull Date timestamp;
    public final @Nullable Object value;

    public Event(@NotNull String path,
                 @NotNull Date timestamp,
                 @Nullable Object value,
                 @NotNull Map<String, Object> properties) {
        //noinspection ConstantValue
        if (path == null)
            throw new IllegalArgumentException("path can not be null");
        if (path.isEmpty())
            throw new IllegalArgumentException("path can not be empty");
        this.path = path;

        //noinspection ConstantValue
        if (timestamp == null)
            throw new IllegalArgumentException("timestamp can not be null");
        this.timestamp = timestamp;

        this.value = value;

        //noinspection ConstantValue
        if (properties == null)
            throw new IllegalArgumentException("properties can not be null");
        _properties = properties;
    }

    public Event(@NotNull String path,
                 @NotNull Date timestamp,
                 @Nullable Object value) {
        //noinspection ConstantValue
        if (path == null)
            throw new IllegalArgumentException("path cannot be null");
        if (path.isEmpty())
            throw new IllegalArgumentException("path cannot be empty");
        this.path = path;

        //noinspection ConstantValue
        if (timestamp == null)
            throw new IllegalArgumentException("timestamp cannot be null");
        this.timestamp = timestamp;

        this.value = value;

        _properties = new HashMap<>();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Event dataEvent = (Event) obj;
        return Objects.equals(path, dataEvent.path) &&
                Objects.equals(timestamp, dataEvent.timestamp) &&
                Objects.equals(value, dataEvent.value);
    }

    public Object getPropertyValue(@NotNull String propertyName) {
        //noinspection ConstantValue
        if (propertyName == null)
            throw new IllegalStateException("propertyName can not be null");
        return _properties.get(propertyName);
    }

    public boolean hasProperty(@NotNull String propertyName) {
        //noinspection ConstantValue
        if (propertyName == null)
            throw new IllegalStateException("propertyName can not be null");
        return _properties.containsKey(propertyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, timestamp, value);
    }

    private final @NotNull Map<String, Object> _properties;
}
