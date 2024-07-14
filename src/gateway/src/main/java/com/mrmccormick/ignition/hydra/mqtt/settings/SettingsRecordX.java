package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.inductiveautomation.ignition.gateway.localdb.persistence.*;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import simpleorm.dataset.SFieldFlags;

import java.sql.SQLException;

public class SettingsRecordX extends PersistentRecord {

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
        settingsRecord.setTagProviderSub("");

        settingsRecord.setBrokerHost("");
        settingsRecord.setBrokerPort(1883);
        settingsRecord.setBrokerSubscribeQos(0);
        settingsRecord.setBrokerSubscriptions("#");

        settingsRecord.setRepresentationSubscribeValuePath("Value");
        settingsRecord.setRepresentationSubscribeTimestampPath("");
    }

    public void Validate() throws Exception {
        var subTagProvider = getTagProviderSub();
        if (subTagProvider == null) {
            throw new Exception("Subscribe-Only Tag Provider must be specified");
        }
        if (getBrokerSubscribeQos() < 0 || getBrokerSubscribeQos() > 2) {
            throw new Exception("Broker Subscribe QoS must be between 0 and 2");
        }
    }


    // Tag Provider Configuration
    public static final StringField TagProviderSub = new StringField(META, "TagProviderSub");

    static final Category TagProviders = new Category(BUNDLE_PREFIX + ".Category.TagProviders", 1000)
            .include(TagProviderSub);

    public void setTagProviderSub(String value) {
        setString(TagProviderSub, value);
    }

    public String getTagProviderSub() {
        return getString(TagProviderSub);
    }


    // Broker Configuration
    public static final StringField BrokerHost = new StringField(META, "BrokerHost", SFieldFlags.SMANDATORY);
    public static final IntField BrokerPort = new IntField(META, "BrokerPort", SFieldFlags.SMANDATORY).setDefault(1883);
    public static final IntField BrokerSubscribeQos = new IntField(META, "BrokerSubscribeQoS", SFieldFlags.SMANDATORY).setDefault(0);
    public static final StringField BrokerSubscriptions = new StringField(META, "BrokerSubscriptions").setDefault("#").setMultiLine();

    static final Category Broker = new Category(BUNDLE_PREFIX + ".Category.Broker", 1001)
            .include(BrokerHost, BrokerPort, BrokerSubscribeQos, BrokerSubscriptions);

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


    // Representation Configuration
    public static final StringField RepresentationSubscribeValuePath = new StringField(META,
            "RepresentationSubscribeValuePath", SFieldFlags.SMANDATORY)
            .setDefault("Value");
    public static final StringField RepresentationSubscribeTimestampPath = new StringField(META,
            "RepresentationSubscribeTimestampPath");

    static final Category Representation = new Category(BUNDLE_PREFIX + ".Category.Representation", 1002)
            .include(RepresentationSubscribeValuePath, RepresentationSubscribeTimestampPath);

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
}
