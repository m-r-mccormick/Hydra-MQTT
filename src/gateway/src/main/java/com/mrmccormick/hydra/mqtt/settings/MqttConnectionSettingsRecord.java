package com.mrmccormick.hydra.mqtt.settings;

import com.mrmccormick.hydra.mqtt.domain.settings.IConnectionSettingsProvider;
import com.mrmccormick.hydra.mqtt.domain.settings.TimestampFormat;
import com.mrmccormick.hydra.mqtt.domain.settings.TimestampIntegerFormat;
import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;

import java.sql.SQLException;
import java.util.*;

import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import simpleorm.dataset.SFieldFlags;

// Class name must be the same as the .properties file but without the .properties extension
public class MqttConnectionSettingsRecord extends PersistentRecord implements IConnectionSettingsProvider {

    // This must match the callouts in the .properties file, e.g.:
    //      BUNDLE_REFERENCE_NAME.Category.Configuration=Configuration
    public static final String BUNDLE_REFERENCE_NAME = MqttConnectionSettingsRecord.class.getSimpleName();

    // If this is not the same as the class name, it messes up the section headers
    public static final String BUNDLE_PREFIX = MqttConnectionSettingsRecord.class.getSimpleName();

    public static final RecordMeta<MqttConnectionSettingsRecord> META = new RecordMeta<>(
            MqttConnectionSettingsRecord.class, BUNDLE_REFERENCE_NAME)
            .setNounKey(BUNDLE_PREFIX + ".Noun")
            .setNounPluralKey(BUNDLE_PREFIX + ".Noun.Plural");

    @Override
    public RecordMeta<?> getMeta() {
        return META;
    }

    public static final IdentityField Id = new IdentityField(META);

    public void setId(Long id) {
        setLong(Id, id);
    }

    public Long getId() {
        return getLong(Id);
    }

    public static void UpdateSchema(GatewayContext context) throws SQLException {
        context.getSchemaUpdater().updatePersistentRecords(META);
    }








    private ConnectionSettingsChangeHandler _changeHandler = null;

    private void setChangeHandler(@NotNull ConnectionSettingsChangeHandler handler) {
        //noinspection ConstantValue
        if (handler == null)
            throw new IllegalArgumentException("handler can not be null");
        _changeHandler = handler;
    }

    public @NotNull ConnectionSettingsChangeHandler getChangeHandler() {
        if (_changeHandler == null) {
            _changeHandler = new ConnectionSettingsChangeHandler();
            if (META.getRecordListenerCount() != 0)
                throw new RuntimeException("Record Listener is not null");
            //noinspection unchecked
            META.addRecordListener(_changeHandler);
        }
        return _changeHandler;
    }

    private GatewayContext _gatewayContext;

    public void setGatewayContext(@NotNull GatewayContext gatewayContext) {
        //noinspection ConstantValue
        if (gatewayContext == null)
            throw new IllegalArgumentException("gatewayContext can not be null");

        _gatewayContext = gatewayContext;
    }

    public @NotNull IConnectionSettingsProvider reload() {
        var record = _gatewayContext.getLocalPersistenceInterface().find(META, 0L);
        record.setGatewayContext(_gatewayContext);
        record.setChangeHandler(_changeHandler);
        return record;
    }

    public @NotNull ConnectionSettings getSettings() {
        if (_gatewayContext == null)
            throw new IllegalStateException("_gatewayContext has not been set");

        return new ConnectionSettings(
                _gatewayContext,
                getConnectionEnabled(),
                getBrokerHost(),
                getBrokerPort(),
                getBrokerPublishQos(),
                getBrokerSubscribeQos(),
                getBrokerSubscriptions(),
                getTagProviderPub(),
                getTagProviderSub(),
                getBrokerPublishTopicSuffix(),
                getTagProviderPublishRoutingPropertiesEnabled(),
                getRepresentationPublishValuePath(),
                getRepresentationPublishTimestampPath(),
                getRepresentationPublishTimestampFormat(),
                getRepresentationPublishDocumentationPath(),
                getRepresentationPublishUnitsPath(),
                getRepresentationSubscribeValuePath(),
                getRepresentationSubscribeTimestampPath(),
                getRepresentationSubscribeTimestampIntegerFormat(),
                getRepresentationSubscribeDocumentationPaths(),
                getRepresentationSubscribeUnitsPaths(),
                getExperimentalGlobalSubscription()
        );
    }






    public static final BooleanField ConnectionEnabled = new BooleanField(META, "ConnectionEnabled",
            SFieldFlags.SMANDATORY).setDefault(false);
    public void setConnectionEnabled(Boolean value) {
        setBoolean(ConnectionEnabled, value);
    }
    public @Nullable Boolean getConnectionEnabled() {
        return getBoolean(ConnectionEnabled);
    }


    static final Category Enabled = new Category(BUNDLE_PREFIX + ".Category.Connection", 1000)
            .include(ConnectionEnabled);












    // Broker Configuration
    public static final StringField BrokerHost = new StringField(META, "BrokerHost", SFieldFlags.SMANDATORY)
            .setDefault("");
    public void setBrokerHost(String value) {
        setString(BrokerHost, value);
    }
    public @Nullable String getBrokerHost() {
        return getString(BrokerHost);
    }


    public static final IntField BrokerPort = new IntField(META, "BrokerPort", SFieldFlags.SMANDATORY)
            .setDefault(1883);
    public void setBrokerPort(int value) {
        setInt(BrokerPort, value);
    }
    public @Nullable Integer getBrokerPort() {
        return getInt(BrokerPort);
    }


    public static final IntField BrokerPublishQos = new IntField(META, "BrokerPublishQoS",
            SFieldFlags.SMANDATORY).setDefault(0).addValidator((IValidator) iValidatable -> {
        for (var error : ConnectionSettings.validateQos("Publish QoS", iValidatable.getValue()))
            iValidatable.error(new ValidationError(error));
    });
    public void setBrokerPublishQos(int value) {
        setInt(BrokerPublishQos, value);
    }
    public @Nullable Integer getBrokerPublishQos() {
        return getInt(BrokerPublishQos);
    }


    public static final IntField BrokerSubscribeQos = new IntField(META, "BrokerSubscribeQoS",
            SFieldFlags.SMANDATORY).setDefault(0).addValidator((IValidator) iValidatable -> {
        for (var error : ConnectionSettings.validateQos("Subscribe QoS", iValidatable.getValue()))
            iValidatable.error(new ValidationError(error));
    });
    public void setBrokerSubscribeQos(int value) {
        setInt(BrokerSubscribeQos, value);
    }
    public @Nullable Integer getBrokerSubscribeQos() {
        return getInt(BrokerSubscribeQos);
    }


    public static final StringField BrokerSubscriptions = new StringField(META, "BrokerSubscriptions")
            .setDefault("#").setMultiLine();
    public void setBrokerSubscriptions(String value) {
        setString(BrokerSubscriptions, value);
    }
    public @Nullable String getBrokerSubscriptions() {
        return getString(BrokerSubscriptions);
    }


    static final Category Broker = new Category(BUNDLE_PREFIX + ".Category.Broker", 1001)
            .include(BrokerHost, BrokerPort, BrokerPublishQos, BrokerSubscribeQos, BrokerSubscriptions);
















    // Tag Provider Configuration
    public static final StringField TagProviderPub = new StringField(META, "TagProviderPub").setDefault("");
    public void setTagProviderSub(String value) {
        setString(TagProviderSub, value);
    }
    public @Nullable String getTagProviderSub() {
        return getString(TagProviderSub);
    }


    public static final StringField TagProviderSub = new StringField(META, "TagProviderSub").setDefault("");
    public void setTagProviderPub(String value) {
        setString(TagProviderPub, value);
    }
    public @Nullable String getTagProviderPub() {
        return getString(TagProviderPub);
    }


    static final Category TagProviders = new Category(BUNDLE_PREFIX + ".Category.TagProviders", 1003)
            .include(TagProviderPub, TagProviderSub);























    public static final StringField BrokerPublishTopicSuffix = new StringField(META,
            "BrokerPublishTopicSuffix").setDefault("");
    public void setBrokerPublishTopicSuffix(String value) {
        setString(BrokerPublishTopicSuffix, value);
    }
    public @Nullable String getBrokerPublishTopicSuffix() {
        return getString(BrokerPublishTopicSuffix);
    }


    public static final BooleanField TagProviderPublishRoutingPropertiesEnabled = new BooleanField(META,
            "TagProviderPublishRoutingPropertiesEnabled").setDefault(true);
    public void setTagProviderPublishRoutingPropertiesEnabled(Boolean value) {
        setBoolean(TagProviderPublishRoutingPropertiesEnabled, value);
    }
    public @Nullable Boolean getTagProviderPublishRoutingPropertiesEnabled() {
        return getBoolean(TagProviderPublishRoutingPropertiesEnabled);
    }


    static final Category Routing = new Category(BUNDLE_PREFIX + ".Category.Routing", 1004)
            .include(BrokerPublishTopicSuffix, TagProviderPublishRoutingPropertiesEnabled);










    // Representation Configuration
    public static final StringField RepresentationPublishValuePath = new StringField(META,
            "RepresentationPublishValuePath", SFieldFlags.SMANDATORY)
            .setDefault("Value").addValidator((IValidator) iValidatable -> {
                var valuesString = (String) iValidatable.getValue();
                if (valuesString == null || valuesString.isEmpty()) {
                    iValidatable.error(new ValidationError("'Publish Value Path' can not be empty."));
                    return;
                }
                List<String> values = new ArrayList<>();
                for (var value : valuesString.split("\r\n")) {
                    // Skip blank lines
                    if (value == null || value.isEmpty())
                        continue;
                    values.add(value);
                }
                if (values.isEmpty())
                    iValidatable.error(new ValidationError("'Publish Value Path' can not be empty."));
            });
    public void setRepresentationPublishValuePath(String value) { setString(RepresentationPublishValuePath, value); }
    public @Nullable String getRepresentationPublishValuePath() {
        return getString(RepresentationPublishValuePath);
    }


    public static final StringField RepresentationPublishTimestampPath = new StringField(META,
            "RepresentationPublishTimestampPath", SFieldFlags.SMANDATORY)
            .setDefault("Timestamp");
    public void setRepresentationPublishTimestampPath(String value) {
        setString(RepresentationPublishTimestampPath, value);
    }
    public @Nullable String getRepresentationPublishTimestampPath() {
        return getString(RepresentationPublishTimestampPath);
    }


    @SuppressWarnings("unchecked")
    public static final EnumField RepresentationPublishTimestampFormat =
            new EnumField(META, "RepresentationPublishTimestampFormat", TimestampFormat.class)
                    .setDefault(TimestampFormat.ISO8601);
    public void setRepresentationPublishTimestampFormat(TimestampFormat value) {
        //noinspection unchecked
        setEnum(RepresentationPublishTimestampFormat, value);
    }
    public @Nullable TimestampFormat getRepresentationPublishTimestampFormat() {
        Enum value;
        try {
            //noinspection unchecked
            value = getEnum(RepresentationPublishTimestampFormat);
        } catch (Exception e) {
            return null;
        }
        if (value == null)
            return null;
        if (!(value instanceof TimestampFormat))
            return null;
        return (TimestampFormat)value;
    }


    public static final StringField RepresentationPublishDocumentationPath = new StringField(META,
            "RepresentationPublishDocumentationPath").setDefault("");
    public void setRepresentationPublishDocumentationPath(String value) {
        setString(RepresentationPublishDocumentationPath, value);
    }
    public @Nullable String getRepresentationPublishDocumentationPath() {
        return getString(RepresentationPublishDocumentationPath);
    }

    public static final StringField RepresentationPublishUnitsPath = new StringField(META,
            "RepresentationPublishUnitsPath").setDefault("");
    public void setRepresentationPublishUnitsPath(String value) {
        setString(RepresentationPublishUnitsPath, value);
    }
    public @Nullable String getRepresentationPublishUnitsPath() {
        return getString(RepresentationPublishUnitsPath);
    }

    public static final StringField RepresentationSubscribeValuePath = new StringField(META,
            "RepresentationSubscribeValuePath", SFieldFlags.SMANDATORY)
            .setDefault("Value").setMultiLine().addValidator((IValidator) iValidatable -> {
                var valuesString = (String) iValidatable.getValue();
                if (valuesString == null || valuesString.isEmpty()) {
                    iValidatable.error(new ValidationError("'Subscribe Value Path' can not be empty."));
                    return;
                }
                List<String> paths = new ArrayList<>();
                for (var value : valuesString.split("\r\n")) {
                    // Skip blank lines
                    if (value == null || value.isEmpty())
                        continue;
                    paths.add(value);
                }
                if (paths.isEmpty())
                    iValidatable.error(new ValidationError("'Subscribe Value Path' line can not be empty."));
            });
    public void setRepresentationSubscribeValuePath(String value) {
        setString(RepresentationSubscribeValuePath, value);
    }
    public @Nullable String getRepresentationSubscribeValuePath() {
        return getString(RepresentationSubscribeValuePath);
    }


    public static final StringField RepresentationSubscribeTimestampPath = new StringField(META,
            "RepresentationSubscribeTimestampPath").setDefault("").setMultiLine();
    public void setRepresentationSubscribeTimestampPath(String value) {
        setString(RepresentationSubscribeTimestampPath, value);
    }
    public @Nullable String getRepresentationSubscribeTimestampPath() {
        return getString(RepresentationSubscribeTimestampPath);
    }


    @SuppressWarnings("unchecked")
    public static final EnumField RepresentationSubscribeTimestampIntegerFormat =
            new EnumField(META, "RepresentationSubscribeTimestampIntegerFormat",
                    TimestampIntegerFormat.class).setDefault(TimestampIntegerFormat.UnixEpochNanoseconds);
    public void setRepresentationSubscribeTimestampIntegerFormat(TimestampIntegerFormat value) {
        //noinspection unchecked
        setEnum(RepresentationSubscribeTimestampIntegerFormat, value);
    }
    public @Nullable TimestampIntegerFormat getRepresentationSubscribeTimestampIntegerFormat() {
        Enum value;
        try {
            //noinspection unchecked
            value = getEnum(RepresentationSubscribeTimestampIntegerFormat);
        } catch (Exception e) {
            return null;
        }
        if (value == null)
            return null;
        if (!(value instanceof TimestampIntegerFormat))
            return null;
        return (TimestampIntegerFormat) value;
    }


    public static final StringField RepresentationSubscribeDocumentationPaths = new StringField(META,
            "RepresentationSubscribeDocumentationPaths").setDefault("").setMultiLine();
    public void setRepresentationSubscribeDocumentationPaths(String value) {
        setString(RepresentationSubscribeDocumentationPaths, value);
    }
    public @Nullable String getRepresentationSubscribeDocumentationPaths() {
        return getString(RepresentationSubscribeDocumentationPaths);
    }


    public static final StringField RepresentationSubscribeUnitsPaths = new StringField(META,
            "RepresentationSubscribeUnitsPaths").setDefault("").setMultiLine();
    public void setRepresentationSubscribeUnitsPaths(String value) {
        setString(RepresentationSubscribeUnitsPaths, value);
    }
    public @Nullable String getRepresentationSubscribeUnitsPaths() {
        return getString(RepresentationSubscribeUnitsPaths);
    }


    static final Category Representation = new Category(BUNDLE_PREFIX + ".Category.Representation", 1006)
            .include(RepresentationPublishValuePath,
                    RepresentationPublishTimestampPath,
                    RepresentationPublishTimestampFormat,
                    RepresentationPublishDocumentationPath,
                    RepresentationPublishUnitsPath,
                    RepresentationSubscribeValuePath,
                    RepresentationSubscribeTimestampPath,
                    RepresentationSubscribeTimestampIntegerFormat,
                    RepresentationSubscribeDocumentationPaths,
                    RepresentationSubscribeUnitsPaths
            );






    public static final BooleanField ExperimentalGlobalSubscription = new BooleanField(META, "ExperimentalGlobalSubscription",
            SFieldFlags.SMANDATORY).setDefault(false);
    public void setExperimentalGlobalSubscription(Boolean value) {
        setBoolean(ExperimentalGlobalSubscription, value);
    }
    public @Nullable Boolean getExperimentalGlobalSubscription() {
        return getBoolean(ExperimentalGlobalSubscription);
    }

    static final Category Experimental = new Category(BUNDLE_PREFIX + ".Category.Experimental", 1007)
            .include(ExperimentalGlobalSubscription
            );






}
