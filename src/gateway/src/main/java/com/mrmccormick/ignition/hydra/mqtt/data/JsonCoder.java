package com.mrmccormick.ignition.hydra.mqtt.data;

import com.mrmccormick.ignition.hydra.mqtt.GatewayHook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import java.security.KeyException;
import java.time.*;
import java.util.*;

public class JsonCoder implements DataCoder {

    private final ObjectMapper _mapper = new ObjectMapper();
    private final Logger _logger = GatewayHook.GetLogger(getClass());
    private final String SubscribeValuePath;
    private final String SubscribeTimestampPath;

    public JsonCoder(String subscribeValuePath, String subscribeTimestampPath) {
        if (subscribeValuePath == null)
            throw new IllegalArgumentException("subscribeValuePath cannot be null");
        if (subscribeValuePath.isEmpty())
            throw new IllegalArgumentException("subscribeValuePath cannot be empty");
        SubscribeValuePath = subscribeValuePath;

        if (subscribeTimestampPath != null)
            if (subscribeTimestampPath.isEmpty())
                throw new IllegalArgumentException("subscribeTimestampPath cannot be empty");
        SubscribeTimestampPath = subscribeTimestampPath;
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

        Map map;
        try {
            map = _mapper.readValue(json, Map.class);
            //map = _gson.fromJson(json, Map.class);
        } catch (Exception e) {
            _logger.error("Could not parse json (" + e.getMessage() + "): " + json, e);
            throw new Exception("Could not parse json (" + e.getMessage() + "): " + json, e);
        }

        Date timestamp;
        if (SubscribeTimestampPath == null) {
            timestamp = Date.from(Instant.now());
        } else {
            var timestampPath = new LinkedList<>(Arrays.stream(SubscribeTimestampPath.split("\\.")).toList());
            Object timestampObject = PathDecode(map, timestampPath);
            Date tryTimestamp = TryObjectToDate(timestampObject);
            if (tryTimestamp == null)
                throw new Exception("Could not parse timestamp at " + SubscribeTimestampPath);

            if (tryTimestamp.before(Date.from(Instant.now()))) {
                timestamp = tryTimestamp;
            } else {
                _logger.warn("Timestamp (" + tryTimestamp + ") at " + path +
                        " must be before now, setting timestamp to now.");
                timestamp = Date.from(Instant.now());
            }
        }

        var valuePath = new LinkedList<>(Arrays.stream(SubscribeValuePath.split("\\.")).toList());
        Object valueObject = PathDecode(map, valuePath);

        Object value;
        Object tryValue = TryObjectToDate(valueObject);
        if (tryValue == null)
            value = valueObject;
        else
            value = tryValue;

        return new DataEvent(DataEventType.Inbound, path, timestamp, value);
    }

    private Date TryObjectToDate(Object object) {
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
