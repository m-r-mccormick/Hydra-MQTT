package com.mrmccormick.ignition.hydra.mqtt.data;

import com.mrmccormick.ignition.hydra.mqtt.GatewayHook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import java.security.KeyException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JsonCoder implements IDataCoder {

    private final ObjectMapper _mapper = new ObjectMapper();
    private final Logger _logger = GatewayHook.GetLogger(getClass());
    public final String SubscribeValuePath;
    public final String SubscribeTimestampPath;
    public final String PublishValuePath;
    public final String PublishTimestampPath;
    public final TimestampFormat PublishFormat;
    public final TimestampIntegerFormat SubscribeFormat;

    public JsonCoder(String subscribeValuePath,
                     String subscribeTimestampPath,
                     String publishValuePath,
                     String publishTimestampPath,
                     TimestampFormat publishFormat,
                     TimestampIntegerFormat subscribeFormat) {
        if (subscribeValuePath == null)
            throw new IllegalArgumentException("subscribeValuePath cannot be null");
        if (subscribeValuePath.isEmpty())
            throw new IllegalArgumentException("subscribeValuePath cannot be empty");
        SubscribeValuePath = subscribeValuePath;

        if (subscribeTimestampPath != null)
            if (subscribeTimestampPath.isEmpty())
                throw new IllegalArgumentException("subscribeTimestampPath cannot be empty");
        SubscribeTimestampPath = subscribeTimestampPath;

        if (publishValuePath == null)
            throw new IllegalArgumentException("publishValuePath cannot be null");
        if (publishValuePath.isEmpty())
            throw new IllegalArgumentException("publishValuePath cannot be empty");
        PublishValuePath = publishValuePath;

        if (publishTimestampPath == null)
            throw new IllegalArgumentException("publishTimestampPath cannot be null");
        if (publishTimestampPath.isEmpty())
            throw new IllegalArgumentException("publishTimestampPath cannot be empty");
        PublishTimestampPath = publishTimestampPath;

        PublishFormat = publishFormat;
        SubscribeFormat = subscribeFormat;
    }

    @Override
    public byte[] Encode(DataEvent event) throws Exception {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        Object timestamp = DateToObject(event.Timestamp);

        Map<String, Object> map = new HashMap<>();
        var timestampPath = new LinkedList<>(Arrays.stream(PublishTimestampPath.split("\\.")).toList());
        var valuePath = new LinkedList<>(Arrays.stream(PublishValuePath.split("\\.")).toList());
        PathEncode(map, timestampPath, timestamp);
        PathEncode(map, valuePath, event.Value);

        String json;
        try {
            json = _mapper.writeValueAsString(map);
        } catch (Exception e) {
            _logger.error("Could not serialize map to json (" + e.getMessage() + "): " +
                    timestamp + ", " + event.Value.toString(), e);
            throw new Exception("Could not serialize map to json (" + e.getMessage() + "): " +
                    timestamp + ", " + event.Value.toString(), e);
        }

        byte[] bytes;
        try {
            bytes = json.getBytes();
        } catch (Exception e) {
            _logger.error("Could not convert json to bytes (" + e.getMessage() + "): " + json, e);
            throw new Exception("Could not convert json to bytes (" + e.getMessage() + "): " + json, e);
        }

        return bytes;
    }

    private void PathEncode(Map<String, Object> parent, List<String> paths, Object value) throws Exception {
        var path = paths.remove(0);
        if (paths.isEmpty()) {
            if (parent.containsKey(path)) {
                throw new KeyException("path key already exists: " + path);
            }
            parent.put(path, value);
            return;
        }

        Map<String, Object> child;
        if (parent.containsKey(path)) {
            Object obj = parent.get(path);
            if (!(obj instanceof Map)) {
                throw new Exception("Expected Map type not found");
            }
            //noinspection unchecked
            child = (Map<String, Object>) obj;
        } else {
            child = new HashMap<>();
        }
        PathEncode(child, paths, value);
        parent.put(path, child);
    }

    private Object PathDecode(Map parent, List<String> paths) throws Exception {
        Map current = parent;
        while (paths.size() > 1) {
            var path = paths.remove(0);
            if (!current.containsKey(path))
                throw new KeyException("Expected Key " + path + " Not Found");
            var currentObj = current.get(path);
            if (!(currentObj instanceof Map))
                throw new Exception("Expected Type Map Not Found");
            current = (Map)currentObj;
        }
        var path = paths.remove(0);
        if (!current.containsKey(path))
            throw new KeyException("Expected Key " + path + " Not Found");
        return current.get(path);
    }

    @Override
    public DataEvent Decode(String path, byte[] bytes) throws Exception {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes can not be null");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("bytes can not be empty");
        }

        String json;
        try {
            json = new String(bytes);
        } catch (Exception e) {
            _logger.error("Could not convert byte array to string");
            throw new Exception("Could not convert byte array to string", e);
        }

        //noinspection rawtypes
        Map map;
        try {
            map = _mapper.readValue(json, Map.class);
        } catch (Exception e) {
            _logger.error("Could not parse json (" + e.getMessage() + "): " + json, e);
            throw new Exception("Could not parse json (" + e.getMessage() + "): " + json, e);
        }

        Date timestamp;
        if (SubscribeTimestampPath == null) {
            timestamp = Date.from(Instant.now());
        } else {
            var timestampPath = new LinkedList<>(Arrays.stream(SubscribeTimestampPath.split("\\.")).toList());
            Date tryTimestamp = ObjectToDate(PathDecode(map, timestampPath));
            if (tryTimestamp == null)
                throw new Exception("Could not parse timestamp at " + SubscribeTimestampPath);

            // When writing a tag to a tag provider, if the timestamp is in the future, ignition will silently change
            //  the timestamp to the current time. This can cause unexpected behavior when one timestamp is written
            //  then another is later received for the same instance of a value/event. So, to ensure that timestamps
            //  are consistent throughout the pipeline for a given instance of a value/event, set any future payload
            //  timestamps to be the current time to preempt this behavior and ensure consistent timestamps throughout
            //  the pipeline.
            var now = Date.from(Instant.now());
            if (tryTimestamp.before(now)) {
                // The payload timestamp is in the past, so use it
                timestamp = tryTimestamp;
            } else {
                // The payload timestamp is in the future, so replace it with the current time
                timestamp = now;
            }
        }

        var valuePath = new LinkedList<>(Arrays.stream(SubscribeValuePath.split("\\.")).toList());
        Object valueObject = PathDecode(map, valuePath);

        Object value;
        Object tryValue = TryDateStringToDate(valueObject);
        if (tryValue == null)
            value = valueObject;
        else
            value = tryValue;

        return new DataEvent(path, timestamp, value);
    }

    private Object DateToObject(Date date) throws Exception {
        switch (PublishFormat) {
            case ISO8601 -> {
                DateTimeFormatter formatter = DateTimeFormatter
                        //.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'")
                        .withZone(ZoneOffset.UTC);

                String timestampString;
                try {
                    timestampString = formatter.format(date.toInstant());
                } catch (Exception e) {
                    _logger.error("Could not format timestamp (" + e.getMessage() + "): " + date.toString(), e);
                    throw new Exception("Could not format timestamp (" + e.getMessage() + "): " + date, e);
                }
                return timestampString;
            }
            case DoubleUnixEpochSeconds -> {
                Instant instant = date.toInstant();
                return instant.getEpochSecond() + (instant.getNano() / 1_000_000_000.0);
            }
            case IntegerUnixEpochSeconds -> {
                Instant instant = date.toInstant();
                return instant.getEpochSecond();
            }
            case IntegerUnixEpochNanoseconds -> {
                Instant instant = date.toInstant();
                return (instant.getEpochSecond() * 1_000_000_000L) + instant.getNano();
            }
            default -> {
                _logger.warn("Invalid TimestampFormat: " + PublishFormat);
                throw new Exception("Invalid TimestampFormat: " + PublishFormat);
            }
        }
    }

    private Date ObjectToDate(Object object) throws Exception {
        if (object instanceof Date) {
            return (Date)object;
        }
        if (object instanceof String) {
            return TryDateStringToDate(object);
        }
        if (object instanceof Integer || object instanceof Long) {
            long ts = ((Number)object).longValue();
            long s; // Seconds part
            long ns; // Nanoseconds part
            switch (SubscribeFormat) {
                case UnixEpochSeconds -> {
                    s = ts; // Seconds part
                    ns = 0L; // Nanoseconds part
                }
                case UnixEpochNanoseconds -> {
                    s = ts / 1_000_000_000L; // Seconds part
                    ns = ts % 1_000_000_000L; // Nanoseconds part
                }
                default -> {
                    _logger.warn("Invalid TimestampIntegerFormat: " + SubscribeFormat);
                    throw new Exception("Invalid TimestampIntegerFormat: " + SubscribeFormat);
                }
            }

            Instant instant = Instant.ofEpochSecond(s, ns);
            return Date.from(instant);
        }
        if (object instanceof Double) {
            long s = (long)((double)object); // Seconds part
            long ns = (long)(((double)object - (double)s) * (double)1_000_000_000); // Nanoseconds part
            Instant instant = Instant.ofEpochSecond(s, ns);
            return Date.from(instant);
        }

        _logger.warn("Invalid Date Type: " + object.getClass().getName());
        throw new Exception("Invalid Date Type: " + object.getClass().getName());
    }

    private Date TryDateStringToDate(Object object) {
        if (!(object instanceof String))
            return null;

        Date ts;
        try {
            OffsetDateTime offsetDateTime = OffsetDateTime.parse((String) object);
            ts = Date.from(offsetDateTime.toInstant());
        } catch (Exception e) {
            // Do nothing
            return null;
        }
        return ts;
    }
}
