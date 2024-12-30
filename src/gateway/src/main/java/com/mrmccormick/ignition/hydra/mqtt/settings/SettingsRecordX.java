package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.mrmccormick.ignition.hydra.mqtt.*;
import com.mrmccormick.ignition.hydra.mqtt.data.JsonCoder;
import com.mrmccormick.ignition.hydra.mqtt.data.TimestampFormat;
import com.mrmccormick.ignition.hydra.mqtt.data.TimestampIntegerFormat;
import org.apache.log4j.Logger;
import simpleorm.dataset.SFieldFlags;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SettingsRecordX extends PersistentRecord implements IConnectSettings {

    private final Logger _logger = GatewayHook.GetLogger(getClass());

    public static final String BUNDLE_REFERENCE_NAME = SettingsRecordX.class.getSimpleName();

    public static final String BUNDLE_PREFIX = SettingsRecordX.class.getSimpleName();

    public static final RecordMeta<SettingsRecordX> META = new RecordMeta<>(
            SettingsRecordX.class, BUNDLE_REFERENCE_NAME)
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

    public static void SetDefaults(SettingsRecordX settingsRecord) {
        settingsRecord.setTagProviderPub("");
        settingsRecord.setTagProviderSub("");

        settingsRecord.setBrokerHost("");
        settingsRecord.setBrokerPort(1883);
        settingsRecord.setBrokerPublishQos(0);
        settingsRecord.setBrokerSubscribeQos(0);
        settingsRecord.setBrokerSubscriptions("#");
        settingsRecord.setBrokerPublishTopicSuffix("");

        settingsRecord.setRepresentationPublishValuePath("Value");
        settingsRecord.setRepresentationPublishTimestampPath("Timestamp");
        settingsRecord.setRepresentationPublishTimestampFormat(TimestampFormat.ISO8601);
        settingsRecord.setRepresentationSubscribeValuePath("Value");
        settingsRecord.setRepresentationSubscribeTimestampPath("");
        settingsRecord.setRepresentationSubscribeTimestampIntegerFormat(TimestampIntegerFormat.UnixEpochNanoseconds);
    }

    @Override
    public boolean getEnabled() {
        return true;
    }

    @Override
    public Connection getConnection(GatewayContext context) throws Exception {
        var configError = false;

        // The name of the connection being created
        var name = getClass().getSimpleName();
        name = name.replace("SettingsRecord", "");
        name = name.replace(this.getClass().getSimpleName(), "");
        if (name.isBlank())
            name = "0";
        name = "Connection " + name;
        var logPrefix = name + " Configuration Error: ";

        // Broker Section
        var host = getBrokerHost();
        var port = getBrokerPort();
        var pubQos = getBrokerPublishQos();
        var subQos = getBrokerSubscribeQos();;
        var brokerSubscriptions = getBrokerSubscriptions();
        List<String> subscriptions = new ArrayList<>();
        for (String subscription : brokerSubscriptions.split("\r\n"))
            if (!subscription.isBlank())
                subscriptions.add(subscription);

        if (getBrokerPublishQos() < 0 || getBrokerPublishQos() > 2) {
            configError = true;
            _logger.error(logPrefix + "Broker Publish QoS must be between 0 and 2");
        }
        if (getBrokerSubscribeQos() < 0 || getBrokerSubscribeQos() > 2) {
            configError = true;
            _logger.error(logPrefix + "Broker Publish QoS must be between 0 and 2");
        }

        // Tag Provider Section
        var pubProvider = getTagProviderPub();
        var subProvider = getTagProviderSub();

        if (pubProvider == null &&
                subProvider == null) {
            configError = true;
            _logger.error(logPrefix + "Publish-Only Tag Provider or Subscribe-Only Tag Provider must be specified");
        }
        if (pubProvider != null && Objects.equals(pubProvider, subProvider)) {
            configError = true;
            _logger.error(logPrefix + "Publish-Only Tag Provider and Subscribe-Only Tag Provider can not be the same");
        }

        // Routing Section
        var writeSuffix = getBrokerPublishTopicSuffix();

        // Representation Section
        var publishValuePath = getRepresentationPublishValuePath();
        var publishTimestampPath = getRepresentationPublishTimestampPath();
        var publishTimestampFormat = getRepresentationPublishTimestampFormat();
        var subscribeValuePath = getRepresentationSubscribeValuePath();
        var subscribeTimestampPath = getRepresentationSubscribeTimestampPath();
        var subscribeTimestampIntegerFormat = getRepresentationSubscribeTimestampIntegerFormat();





        TagManager tagManagerPublish = null;
        if (pubProvider != null) {
            try {
                tagManagerPublish = new TagManager(context, pubProvider);
            } catch (Exception e) {
                configError = true;
                _logger.error(logPrefix + "Could not configure Publish-Only Tag Provider: " + e.getMessage(), e);
            }
        }

        TagManager tagManagerSubscribe = null;
        if (subProvider != null) {
            try {
                tagManagerSubscribe = new TagManager(context, subProvider);
            } catch (Exception e) {
                configError = true;
                _logger.error(logPrefix + "Could not configure Subscribe-Only Tag Provider: " + e.getMessage(), e);
            }
        }

        JsonCoder coder = null;
        try {
            coder = new JsonCoder(subscribeValuePath, subscribeTimestampPath, publishValuePath,
                    publishTimestampPath, publishTimestampFormat, subscribeTimestampIntegerFormat);
        } catch (Exception e) {
            configError = true;
            _logger.error(logPrefix + "Could not configure JsonCoder: " + e.getMessage(), e);
        }

        MqttManager mqttManager = null;
        try {
            mqttManager = new MqttManager(host, port, pubQos, subQos, coder, writeSuffix, subscriptions);
        } catch (Exception e) {
            configError = true;
            _logger.warn(logPrefix + "Could not configure MqttManager: " + e.getMessage(), e);
        }

        Connection connection = null;
        try {
            connection = new Connection(name, mqttManager, tagManagerPublish, tagManagerSubscribe);
        } catch (Exception e) {
            configError = true;
            _logger.warn(logPrefix + "Could not configure Connection: " + e.getMessage(), e);
        }

        if (configError)
            throw new Exception("Could not configure Connection: " + name);

        return connection;
    }










    // Broker Configuration
    public static final StringField BrokerHost = new StringField(META, "BrokerHost", SFieldFlags.SMANDATORY);
    public static final IntField BrokerPort = new IntField(META, "BrokerPort", SFieldFlags.SMANDATORY).setDefault(1883);
    public static final IntField BrokerPublishQos = new IntField(META, "BrokerPublishQoS", SFieldFlags.SMANDATORY).setDefault(0);
    public static final IntField BrokerSubscribeQos = new IntField(META, "BrokerSubscribeQoS", SFieldFlags.SMANDATORY).setDefault(0);
    public static final StringField BrokerSubscriptions = new StringField(META, "BrokerSubscriptions").setDefault("#").setMultiLine();

    static final Category Broker = new Category(BUNDLE_PREFIX + ".Category.Broker", 1001)
            .include(BrokerHost, BrokerPort, BrokerPublishQos, BrokerSubscribeQos, BrokerSubscriptions);

    public void setBrokerHost(String value) {
        setString(BrokerHost, value);
    }

    public String getBrokerHost() {
        return getString(BrokerHost);
    }

    public void setBrokerPort(int value) {
        setInt(BrokerPort, value);
    }

    public int getBrokerPort() {
        return getInt(BrokerPort);
    }

    public void setBrokerPublishQos(int value) {
        setInt(BrokerPublishQos, value);
    }

    public int getBrokerPublishQos() {
        return getInt(BrokerPublishQos);
    }

    public void setBrokerSubscribeQos(int value) {
        setInt(BrokerSubscribeQos, value);
    }

    public int getBrokerSubscribeQos() {
        return getInt(BrokerSubscribeQos);
    }

    public void setBrokerSubscriptions(String value) {
        setString(BrokerSubscriptions, value);
    }

    public String getBrokerSubscriptions() {
        return getString(BrokerSubscriptions);
    }






    // Tag Provider Configuration
    public static final StringField TagProviderPub = new StringField(META, "TagProviderPub");
    public static final StringField TagProviderSub = new StringField(META, "TagProviderSub");

    static final Category TagProviders = new Category(BUNDLE_PREFIX + ".Category.TagProviders", 1003)
            .include(TagProviderPub, TagProviderSub);

    public void setTagProviderSub(String value) {
        setString(TagProviderSub, value);
    }

    public String getTagProviderSub() {
        return getString(TagProviderSub);
    }

    public void setTagProviderPub(String value) {
        setString(TagProviderPub, value);
    }

    public String getTagProviderPub() {
        return getString(TagProviderPub);
    }

















    public static final StringField BrokerPublishTopicSuffix = new StringField(META, "BrokerPublishTopicSuffix");

    static final Category Routing = new Category(BUNDLE_PREFIX + ".Category.Routing", 1004)
            .include(BrokerPublishTopicSuffix);

    public void setBrokerPublishTopicSuffix(String value) {
        setString(BrokerPublishTopicSuffix, value);
    }

    public String getBrokerPublishTopicSuffix() {
        return getString(BrokerPublishTopicSuffix);
    }






    // Representation Configuration
    public static final StringField RepresentationPublishValuePath = new StringField(META,
            "RepresentationPublishValuePath", SFieldFlags.SMANDATORY)
            .setDefault("Value");
    public static final StringField RepresentationPublishTimestampPath = new StringField(META,
            "RepresentationPublishTimestampPath", SFieldFlags.SMANDATORY)
            .setDefault("Timestamp");
    public static final EnumField RepresentationPublishTimestampFormat = new EnumField(META, "RepresentationPublishTimestampFormat", TimestampFormat.class).setDefault(TimestampFormat.ISO8601);
    public static final StringField RepresentationSubscribeValuePath = new StringField(META,
            "RepresentationSubscribeValuePath", SFieldFlags.SMANDATORY)
            .setDefault("Value");
    public static final StringField RepresentationSubscribeTimestampPath = new StringField(META,
            "RepresentationSubscribeTimestampPath");
    public static final EnumField RepresentationSubscribeTimestampIntegerFormat = new EnumField(META, "RepresentationSubscribeTimestampIntegerFormat", TimestampIntegerFormat.class).setDefault(TimestampIntegerFormat.UnixEpochNanoseconds);


    static final Category Representation = new Category(BUNDLE_PREFIX + ".Category.Representation", 1005)
            .include(RepresentationPublishValuePath, RepresentationPublishTimestampPath,
                    RepresentationPublishTimestampFormat,
                    RepresentationSubscribeValuePath, RepresentationSubscribeTimestampPath,
                    RepresentationSubscribeTimestampIntegerFormat);

    public void setRepresentationPublishValuePath(String value) {
        setString(RepresentationPublishValuePath, value);
    }

    public String getRepresentationPublishValuePath() {
        return getString(RepresentationPublishValuePath);
    }

    public void setRepresentationPublishTimestampPath(String value) {
        setString(RepresentationPublishTimestampPath, value);
    }

    public String getRepresentationPublishTimestampPath() {
        return getString(RepresentationPublishTimestampPath);
    }

    public void setRepresentationPublishTimestampFormat(TimestampFormat value) {
        setEnum(RepresentationPublishTimestampFormat, value);
    }

    public TimestampFormat getRepresentationPublishTimestampFormat() {
        return (TimestampFormat)getEnum(RepresentationPublishTimestampFormat);
    }

    public void setRepresentationSubscribeValuePath(String value) {
        setString(RepresentationSubscribeValuePath, value);
    }

    public String getRepresentationSubscribeValuePath() {
        return getString(RepresentationSubscribeValuePath);
    }

    public void setRepresentationSubscribeTimestampPath(String value) {
        setString(RepresentationSubscribeTimestampPath, value);
    }

    public String getRepresentationSubscribeTimestampPath() {
        return getString(RepresentationSubscribeTimestampPath);
    }

    public void setRepresentationSubscribeTimestampIntegerFormat(TimestampIntegerFormat value) {
        setEnum(RepresentationSubscribeTimestampIntegerFormat, value);
    }

    public TimestampIntegerFormat getRepresentationSubscribeTimestampIntegerFormat() {
        return (TimestampIntegerFormat)getEnum(RepresentationSubscribeTimestampIntegerFormat);
    }








}
