package com.mrmccormick.hydra.mqtt.implementation.actor.connector;

import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.common.Dataset;
import com.inductiveautomation.ignition.common.document.Document;
import com.inductiveautomation.ignition.common.gson.JsonArray;
import com.inductiveautomation.ignition.common.gson.JsonObject;
import com.mrmccormick.hydra.mqtt.GatewayHook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mrmccormick.hydra.mqtt.domain.Event;
import com.mrmccormick.hydra.mqtt.domain.EventProperty;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.ICoder;
import com.mrmccormick.hydra.mqtt.domain.settings.TimestampFormat;
import com.mrmccormick.hydra.mqtt.domain.settings.TimestampIntegerFormat;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.security.KeyException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JsonCoder implements ICoder {

    public final @NotNull List<String> publishDocumentationPath;
    public final TimestampFormat publishFormat;
    public final @NotNull List<String> publishTimestampPath;
    public final @NotNull List<String> publishUnitsPath;
    public final @NotNull List<String> publishValuePath;
    public final @NotNull List<List<String>> subscribeDocumentationPaths;
    public final TimestampIntegerFormat subscribeFormat;
    public final @NotNull List<List<String>> subscribeTimestampPaths;
    public final @NotNull List<List<String>> subscribeUnitsPaths;
    public final @NotNull List<List<String>> subscribeValuePaths;

    public JsonCoder(@NotNull List<List<String>> subscribeValuePaths,
                     @NotNull List<List<String>> subscribeTimestampPaths,
                     @NotNull List<List<String>> subscribeDocumentationPaths,
                     @NotNull List<List<String>> subscribeUnitsPaths,
                     @NotNull TimestampIntegerFormat subscribeFormat,
                     @NotNull List<String> publishValuePath,
                     @NotNull List<String> publishTimestampPath,
                     @NotNull List<String> publishDocumentationPath,
                     @NotNull List<String> publishUnitsPath,
                     @NotNull TimestampFormat publishFormat
    ) {
        //noinspection ConstantValue
        if (subscribeValuePaths == null)
            throw new IllegalArgumentException("subscribeValuePaths can not be null");
        if (subscribeValuePaths.isEmpty())
            throw new IllegalArgumentException("subscribeValuePaths can not be empty");
        this.subscribeValuePaths = subscribeValuePaths;

        //noinspection ConstantValue
        if (subscribeTimestampPaths == null)
            throw new IllegalArgumentException("subscribeTimestampPaths can not be null");
        this.subscribeTimestampPaths = subscribeTimestampPaths;

        //noinspection ConstantValue
        if (subscribeDocumentationPaths == null)
            throw new IllegalArgumentException("subscribeDocumentationPaths can not be null");
        this.subscribeDocumentationPaths = subscribeDocumentationPaths;

        //noinspection ConstantValue
        if (subscribeUnitsPaths == null)
            throw new IllegalArgumentException("subscribeUnitsPaths can not be null");
        this.subscribeUnitsPaths = subscribeUnitsPaths;

        //noinspection ConstantValue
        if (subscribeFormat == null)
            throw new IllegalArgumentException("subscribeFormat can not be null");
        this.subscribeFormat = subscribeFormat;

        //noinspection ConstantValue
        if (publishValuePath == null)
            throw new IllegalArgumentException("publishValuePath can not be null");
        if (publishValuePath.isEmpty())
            throw new IllegalArgumentException("publishValuePath can not be empty");
        this.publishValuePath = publishValuePath;

        //noinspection ConstantValue
        if (publishTimestampPath == null)
            throw new IllegalArgumentException("publishTimestampPath can not be null");
        this.publishTimestampPath = publishTimestampPath;

        //noinspection ConstantValue
        if (publishDocumentationPath == null)
            throw new IllegalArgumentException("publishDocumentationPath can not be null");
        this.publishDocumentationPath = publishDocumentationPath;

        //noinspection ConstantValue
        if (publishUnitsPath == null)
            throw new IllegalArgumentException("publishUnitsPath can not be null");
        this.publishUnitsPath = publishUnitsPath;

        //noinspection ConstantValue
        if (publishFormat == null)
            throw new IllegalArgumentException("publishFormat can not be null");
        this.publishFormat = publishFormat;
    }

    @Override
    public @NotNull Event decode(@NotNull String path, @NotNull byte[] bytes) throws Exception {
        //noinspection ConstantValue
        if (path == null)
            throw new IllegalArgumentException("path can not be null");

        //noinspection ConstantValue
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
            throw new Exception("Could not decode byte array: " + e.getMessage(), e);
        }

        //noinspection rawtypes
        Object o;
        try {
            o = _mapper.readValue(json, Map.class);
        } catch (Exception e1) {
            try {
                o = _mapper.readValue(json, List.class);
            } catch (Exception e2) {
                throw new Exception("Could not parse json (" + e1.getMessage() + ", " + e1.getMessage() + "): " + json, e2);
            }
        }

        Date timestamp = null;
        if (o instanceof Map map) {
            if (subscribeTimestampPaths.isEmpty()) {
                timestamp = Date.from(Instant.now());
            } else {
                for (var subscribeTimestampPath : subscribeTimestampPaths) {
                    var pathValue = tryGetAtPath(map, subscribeTimestampPath);
                    if (pathValue == null)
                        continue;
                    Date tryTimestamp = tryDecodeTimestamp(pathValue);
                    if (tryTimestamp == null)
                        continue;

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
                    break;
                }
                if (timestamp == null)
                    throw new Exception("Could not decode timestamp at any Subscribe Timestamp Paths.");
            }
        } else {
            timestamp = Date.from(Instant.now());
        }

        Object value = null;
        if (o instanceof Map map) {
            for (var subscribeValuePath : subscribeValuePaths) {
                var pathValue = tryGetAtPath(map, subscribeValuePath);
                if (pathValue == null)
                    continue;
                Object tryValue = tryDecode(pathValue);
                if (tryValue == null)
                    continue;
                value = tryValue;
                break;
            }
            if (value == null)
                throw new Exception("Could not decode value at any Subscribe Value Paths.");
        } else if (o instanceof List list) {
            Object tryDecode = tryDecode(list);
            if (tryDecode == null)
                throw new Exception("Could not decode value at any Subscribe Value Paths.");
            value = tryDecode;
        } else {
            throw new Exception("Could not decode value at any Subscribe Value Paths.");
        }

        Map<String, Object> properties = new HashMap<>();
        if (!subscribeDocumentationPaths.isEmpty()) {
            if (o instanceof Map map) {
                for (var subscribePath : subscribeDocumentationPaths) {
                    var pathValue = tryGetAtPath(map, subscribePath);
                    if (pathValue == null)
                        continue;
                    Object tryValue = tryDecode(pathValue);
                    if (tryValue == null)
                        continue;
                    if (!(tryValue instanceof String))
                        continue;
                    properties.put(EventProperty.Documentation.name(), tryValue);
                    break;
                }
            }
        }
        if (!subscribeUnitsPaths.isEmpty()) {
            if (o instanceof Map map) {
                for (var subscribePath : subscribeUnitsPaths) {
                    var pathValue = tryGetAtPath(map, subscribePath);
                    if (pathValue == null)
                        continue;
                    Object tryValue = tryDecode(pathValue);
                    if (tryValue == null)
                        continue;
                    if (!(tryValue instanceof String))
                        continue;
                    properties.put(EventProperty.EngineeringUnits.name(), tryValue);
                    break;
                }
            }
        }

        return new Event(path, timestamp, value, properties);
    }

    @Override
    public @NotNull byte[] encode(@NotNull Event event) throws Exception {
        //noinspection ConstantValue
        if (event == null)
            throw new IllegalArgumentException("event cannot be null");

        Object timestamp = encodeTimestamp(event.timestamp);
        Object value = encode(event.value);

        Map<String, Object> map = new HashMap<>();
        setAtPath(map, publishTimestampPath, timestamp);
        setAtPath(map, publishValuePath, value);
        if (!publishDocumentationPath.isEmpty() && event.hasProperty(EventProperty.Documentation.name()))
            setAtPath(map, publishDocumentationPath, event.getPropertyValue(EventProperty.Documentation.name()));
        if (!publishUnitsPath.isEmpty() && event.hasProperty(EventProperty.EngineeringUnits.name()))
            setAtPath(map, publishUnitsPath, event.getPropertyValue(EventProperty.EngineeringUnits.name()));

        String json;
        try {
            json = _mapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new Exception("Could not serialize map to json (" + e + "): " +
                    timestamp + ", " + event.value, e);
        }

        byte[] bytes;
        try {
            bytes = json.getBytes();
        } catch (Exception e) {
            throw new Exception("Could not convert json to bytes (" + e + "): " + json, e);
        }

        return bytes;
    }

    private final @NotNull DateTimeFormatter _formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'")
            .withZone(ZoneOffset.UTC);
    private final @NotNull Logger _logger = GatewayHook.getLogger(getClass());
    private final @NotNull ObjectMapper _mapper = new ObjectMapper();

    private @Nullable JsonObject buildJsonObjectRecursive(@NotNull Map map) {
        //noinspection ConstantValue
        if (map == null)
            throw new IllegalArgumentException("map cannot be null");

        JsonObject parent = new JsonObject();
        for (var key : map.keySet()) {
            if (!(key instanceof String propertyName))
                return null;
            var v = map.get(key);
            if ((v instanceof Map m)) {
                JsonObject o = buildJsonObjectRecursive(m);
                if (o == null)
                    return null;
                parent.add(propertyName, o);
            } else if ((v instanceof List l)) {
                JsonArray o = buildJsonObjectRecursive(l);
                if (o == null)
                    return null;
                parent.add(propertyName, o);
            } else if ((v instanceof String s))
                parent.addProperty(propertyName, s);
            else if ((v instanceof Number n))
                parent.addProperty(propertyName, n);
            else if ((v instanceof Boolean b))
                parent.addProperty(propertyName, b);
            else if ((v instanceof Character c))
                parent.addProperty(propertyName, c);
            else
                return null;
        }
        return parent;
    }

    private @Nullable JsonArray buildJsonObjectRecursive(@NotNull List list) {
        //noinspection ConstantValue
        if (list == null)
            throw new IllegalArgumentException("list cannot be null");

        JsonArray parent = new JsonArray();
        for (var v : list) {
            if ((v instanceof Map m)) {
                JsonObject o = buildJsonObjectRecursive(m);
                if (o == null)
                    return null;
                parent.add(o);
            } else if ((v instanceof List l)) {
                JsonArray o = buildJsonObjectRecursive(l);
                if (o == null)
                    return null;
                parent.add(o);
            } else if ((v instanceof String s))
                parent.add(s);
            else if ((v instanceof Number n))
                parent.add(n);
            else if ((v instanceof Boolean b))
                parent.add(b);
            else if ((v instanceof Character c))
                parent.add(c);
            else
                return null;
        }
        return parent;
    }

    /**
     * Encode an application object into a JSON-serializable object.
     *
     * @param o The object to encode.
     * @return The object encoded into a JSON-serializable object.
     * @throws Exception Unsupported input object type
     */
    private @Nullable Object encode(@Nullable Object o) throws Exception {
        // Json Natively-Encodable Types
        if (o == null)
            return null;
        if (o instanceof Boolean)
            return o;
        if (o instanceof String)
            return o;
        //noinspection DuplicatedCode
        if (o instanceof Double)
            return o;
        if (o instanceof Float)
            return o;
        if (o instanceof Long)
            return o;
        if (o instanceof Integer)
            return o;
        if (o instanceof Short)
            return o;
        if (o instanceof Byte)
            return o;
        if (o instanceof Boolean[])
            return o;
        if (o instanceof String[])
            return o;
        if (o instanceof Double[])
            return o;
        if (o instanceof Float[])
            return o;
        if (o instanceof Long[])
            return o;
        if (o instanceof Integer[])
            return o;
        if (o instanceof Short[])
            return o;
        if (o instanceof Byte[])
            return o;

        // Non-standard types
        if (o instanceof Date)
            return encodeTimestampIso8601((Date) o);
        if (o instanceof Date[]) {
            List<Object> dates = new ArrayList<>();
            for (var date : (Date[]) o)
                dates.add(encodeTimestampIso8601(date));
            return dates;
        }
        if (o instanceof Document)
            return encodeDocument((Document) o);
        if (o instanceof BasicDataset)
            return encodeDataset((BasicDataset) o);
        throw new Exception("Unsupported Object Type: " + o.getClass().getSimpleName());
    }

    private @NotNull Serializable encodeDataset(@NotNull BasicDataset dataset) {
        //noinspection ConstantValue
        if (dataset == null)
            throw new IllegalArgumentException("dataset cannot be null");

        var data = dataset.getData();
        var columnNames = dataset.getColumnNames();
        List<String> columnTypes = new ArrayList<>();
        for (var type : dataset.getColumnTypes())
            columnTypes.add(type.toString());

        HashMap<String, Object> value = new HashMap<>();
        value.put("Data", data);
        value.put("ColumnNames", columnNames);
        value.put("ColumnTypes", columnTypes);
        return value;
    }

    private @NotNull Serializable encodeDocument(@NotNull Document document) throws Exception {
        //noinspection ConstantValue
        if (document == null)
            throw new IllegalArgumentException("document cannot be null");
        return _mapper.readValue(document.toStringNoIndent(), HashMap.class);
    }

    private @NotNull Object encodeTimestamp(@NotNull Date timestamp) throws Exception {
        //noinspection ConstantValue
        if (timestamp == null)
            throw new IllegalArgumentException("timestamp cannot be null");

        switch (publishFormat) {
            case ISO8601 -> {
                String timestampString;
                try {
                    timestampString = encodeTimestampIso8601(timestamp);
                } catch (Exception e) {
                    _logger.error("Could not format timestamp (" + e.getMessage() + "): " + timestamp, e);
                    throw new Exception("Could not format timestamp (" + e.getMessage() + "): " + timestamp, e);
                }
                return timestampString;
            }
            case DoubleUnixEpochSeconds -> {
                return timestamp.toInstant().getEpochSecond() + (timestamp.toInstant().getNano() / 1_000_000_000.0);
            }
            case IntegerUnixEpochSeconds -> {
                return timestamp.toInstant().getEpochSecond();
            }
            case IntegerUnixEpochNanoseconds -> {
                return (timestamp.toInstant().getEpochSecond() * 1_000_000_000L) + timestamp.toInstant().getNano();
            }
            default -> {
                _logger.warn("Invalid TimestampFormat: " + publishFormat);
                throw new Exception("Invalid TimestampFormat: " + publishFormat);
            }
        }
    }

    private @NotNull String encodeTimestampIso8601(@NotNull Date timestamp) {
        //noinspection ConstantValue
        if (timestamp == null)
            throw new IllegalArgumentException("timestamp cannot be null");
        return _formatter.format(timestamp.toInstant());
    }

    private void setAtPath(@NotNull Map<String, Object> parent, @NotNull List<String> paths, @Nullable Object value) throws Exception {
        //noinspection ConstantValue
        if (parent == null)
            throw new IllegalArgumentException("parent cannot be null");
        //noinspection ConstantValue
        if (paths == null)
            throw new IllegalArgumentException("paths cannot be null");

        var editablePaths = new ArrayList<>(paths);
        var path = editablePaths.remove(0);
        if (editablePaths.isEmpty()) {
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
        setAtPath(child, editablePaths, value);
        parent.put(path, child);
    }

    /**
     * Decode a JSON-deserialized object into an application object.
     *
     * @param o The object to decode.
     * @return The object decode into an application object.
     */
    private @Nullable Object tryDecode(@Nullable Object o) {
        // Json Natively-Decodable Types
        if (o == null)
            return null;
        if (o instanceof Boolean)
            return o;
        if (o instanceof String) {
            // Can be a string or a date
            var ts = tryDecodeTimestamp(o);
            if (ts != null)
                return ts;
            return o;
        }
        //noinspection DuplicatedCode
        if (o instanceof Double)
            return o;
        if (o instanceof Float)
            return o;
        if (o instanceof Long)
            return o;
        if (o instanceof Integer)
            return o;
        if (o instanceof Short)
            return o;
        if (o instanceof Byte)
            return o;
        if (o instanceof List) {
            // Can be an Array or Document (without column names or types)
            var ar = tryDecodeArray((List) o);
            if (ar != null)
                return ar;
            var ds = tryDecodeDataset(o);
            if (ds != null)
                return ds;
        }
        if (o instanceof Map) {
            // Can be a Document or Dataset
            // Try dataset before document because it has a more unique and
            //  constrained schema, whereas a Document is very free-form
            var ds = tryDecodeDataset(o);
            if (ds != null)
                return ds;
            return tryDecodeDocument(o);
        }
        return null;
    }

    private @Nullable Object[] tryDecodeArray(@NotNull List list) {
        //noinspection ConstantValue
        if (list == null)
            throw new IllegalArgumentException("list cannot be null");

        if (list.isEmpty())
            return null;

        var first = list.get(0);
        if (first instanceof Boolean) {
            var a = new Boolean[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Boolean))
                    return null;
                a[i] = (Boolean) list.get(i);
            }
            return a;
        } else if (first instanceof String) {
            var a = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof String))
                    return null;
                a[i] = (String) list.get(i);
            }
            return a;
        } else if (first instanceof Double) {
            var a = new Double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Double))
                    return null;
                a[i] = (Double) list.get(i);
            }
            return a;
        } else if (first instanceof Float) {
            var a = new Float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Float))
                    return null;
                a[i] = (Float) list.get(i);
            }
            return a;
        } else if (first instanceof Long) {
            var a = new Long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Long))
                    return null;
                a[i] = (Long) list.get(i);
            }
            return a;
        } else if (first instanceof Integer) {
            var a = new Integer[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Integer))
                    return null;
                a[i] = (Integer) list.get(i);
            }
            return a;
        } else if (first instanceof Short) {
            var a = new Short[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Short))
                    return null;
                a[i] = (Short) list.get(i);
            }
            return a;
        } else if (first instanceof Byte) {
            var a = new Byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof Byte))
                    return null;
                a[i] = (Byte) list.get(i);
            }
            return a;
        }

        return null;
    }


    private @Nullable Dataset tryDecodeDataset(@NotNull Object object) {
        //noinspection ConstantValue
        if (object == null)
            throw new IllegalArgumentException("object cannot be null");

        Object dataObject = null;
        Object columnNamesObject = null;
        Object columnTypesObject = null;

        if (object instanceof List list) {
            dataObject = list;
        } else if (object instanceof Map map) {
            if (!map.containsKey("Data"))
                return null;
            dataObject = map.get("Data");
            if (map.containsKey("ColumnNames"))
                columnNamesObject = map.get("ColumnNames");
            if (map.containsKey("ColumnTypes"))
                columnTypesObject = map.get("ColumnTypes");
        }

        if (!(dataObject instanceof List dataColumns))
            return null;
        if (dataColumns.isEmpty())
            return null;
        int columns = dataColumns.size();
        var firstRowObject = dataColumns.get(0);
        if (!(firstRowObject instanceof List firstColumn))
            return null;
        int rows = firstColumn.size();
        Object[][] data = new Object[columns][rows];
        List<Class<?>> actualColumnTypes = new ArrayList<>();
        for (int i = 0; i < columns; i++) {
            var columnObject = dataColumns.get(i);
            if (!(columnObject instanceof List column))
                return null;
            if (column.isEmpty())
                return null;
            var columnType = column.get(0).getClass();
            for (int j = 0; j < rows; j++) {
                data[i][j] = column.get(j);
                if (column.get(j).getClass() != columnType)
                    columnType = null;
            }
            actualColumnTypes.add(columnType);
        }

        List<String> columnNames = new ArrayList<>();
        if (columnNamesObject != null) {
            if (!(columnNamesObject instanceof List list))
                return null;
            for (var o : list) {
                if (!(o instanceof String columnName))
                    return null;
                columnNames.add(columnName);
            }
            if (columnNames.size() != columns)
                return null;
        } else {
            for (int i = 0; i < columns; i++)
                columnNames.add("C" + String.format("%d", i));
        }

        List<Class<?>> columnTypes = new ArrayList<>();
        if (columnTypesObject != null) {
            if (!(columnTypesObject instanceof List list))
                return null;
            for (var o : list) {
                if (!(o instanceof String columnTypeName))
                    return null;
                try {
                    columnTypes.add(Class.forName(columnTypeName.replace("class ", "")));
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }
            if (columnTypes.size() != actualColumnTypes.size())
                return null;
            for (int i = 0; i < columnTypes.size(); i++) {
                if (!actualColumnTypes.get(i).equals(columnTypes.get(i)))
                    return null;
            }
        } else {
            if (actualColumnTypes.contains(null))
                return null;
            columnTypes = actualColumnTypes;
        }

        return new BasicDataset(columnNames, columnTypes, data);
    }

    private @Nullable Document tryDecodeDocument(@NotNull Object object) {
        //noinspection ConstantValue
        if (object == null)
            throw new IllegalArgumentException("object cannot be null");

        if (!(object instanceof Map map))
            return null;
        try {
            return new Document(buildJsonObjectRecursive(map));
        } catch (Exception e) {
            return null;
        }
    }

    private @Nullable Date tryDecodeTimestamp(@Nullable Object object) {
        if (object instanceof Instant) {
            return Date.from((Instant) object);
        }
        if (object instanceof Date) {
            return (Date) object;
        }
        if (object instanceof String) {
            try {
                OffsetDateTime offsetDateTime = OffsetDateTime.parse((String) object);
                return Date.from(offsetDateTime.toInstant());
            } catch (Exception e) {
                return null;
            }
        }
        if (object instanceof Integer || object instanceof Long) {
            long ts = ((Number) object).longValue();
            long s; // Seconds part
            long ns; // Nanoseconds part
            switch (subscribeFormat) {
                case UnixEpochSeconds -> {
                    s = ts; // Seconds part
                    ns = 0L; // Nanoseconds part
                }
                case UnixEpochNanoseconds -> {
                    s = ts / 1_000_000_000L; // Seconds part
                    ns = ts % 1_000_000_000L; // Nanoseconds part
                }
                default -> {
                    return null;
                }
            }

            return Date.from(Instant.ofEpochSecond(s, ns));
        }
        if (object instanceof Double) {
            long s = (long) ((double) object); // Seconds part
            long ns = (long) (((double) object - (double) s) * (double) 1_000_000_000); // Nanoseconds part
            return Date.from(Instant.ofEpochSecond(s, ns));
        }

        return null;
    }

    /**
     * Try to get an object from nested maps.
     *
     * @param parent         The parent map to traverse.
     * @param pathComponents The path to the nested object (e.g., `a/b/c` where `a` and `b` are maps,
     *                       and `c` is a value)
     * @return The object at the specified path, or {@code null} if the path is invalid.
     */
    private Object tryGetAtPath(Map parent, List<String> pathComponents) {
        if (parent == null)
            throw new IllegalArgumentException("parent can not be null");
        if (pathComponents == null)
            throw new IllegalArgumentException("pathComponents can not be null");
        List<String> editablePathComponents = new ArrayList<>(pathComponents);

        // Progressively dig deeper into the map until the specified path/depth is reached
        Map current = parent;
        while (editablePathComponents.size() > 1) {
            var path = editablePathComponents.remove(0);
            // Check whether the next level nested map exists
            if (!current.containsKey(path))
                // Doesn't contain the next level nested map
                return null;
            // It does contain the next level nested map, get it
            var currentObj = current.get(path);
            // Check whether the next level nested map is indeed a map
            if (!(currentObj instanceof Map))
                // It is not a map, so can't continue deeper
                return null;
            // It is a map, set that map to current, then repeat
            current = (Map) currentObj;
        }
        var path = editablePathComponents.remove(0);
        // At the specified path/depth, check whether the value exists
        if (!current.containsKey(path))
            // Value doesn't exist
            return null;
        return current.get(path);
    }
}
