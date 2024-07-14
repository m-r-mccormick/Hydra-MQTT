package com.mrmccormick.ignition.hydra.mqtt.data;

import java.util.Date;
import java.util.Objects;

public class DataEvent {

    public final DataEventType Type;
    public final Date Timestamp;
    public final String Path;
    public final Object Value;

    public DataEvent(DataEventType type, String path, Date timestamp, Object value) {
        if (type == null)
            throw new IllegalArgumentException("Type cannot be null");
        Type = type;

        if (path == null)
            throw new IllegalArgumentException("path cannot be null");
        if (path.isEmpty())
            throw new IllegalArgumentException("path cannot be empty");
        Path = path;

        if (timestamp == null)
            throw new IllegalArgumentException("timestamp cannot be null");
        Timestamp = timestamp;

        // Value can be null
        Value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DataEvent dataEvent = (DataEvent) obj;
        return Objects.equals(Path, dataEvent.Path) &&
                Objects.equals(Value, dataEvent.Value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Path, Timestamp, Value);
    }
}
