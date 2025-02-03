package com.mrmccormick.hydra.mqtt.implementation.actor.connector;

import com.google.common.collect.ImmutableList;
import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.config.BasicConfigurationProperty;
import com.inductiveautomation.ignition.common.config.MutableConfigurationPropertyModel;
import com.inductiveautomation.ignition.common.config.PropertyModelContributor;
import com.inductiveautomation.ignition.common.document.Document;
import com.inductiveautomation.ignition.common.i18n.LocalizedString;
import com.inductiveautomation.ignition.common.model.values.BasicQualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.BasicTagConfiguration;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.model.SecurityContext;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.model.TagProviderInformation;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import com.mrmccormick.hydra.mqtt.domain.Event;
import com.mrmccormick.hydra.mqtt.domain.EventProperty;
import com.mrmccormick.hydra.mqtt.domain.actor.*;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.IConnector;
import com.mrmccormick.hydra.mqtt.ignition.ITagChangeSubscriber;
import com.mrmccormick.hydra.mqtt.ignition.TagChangeActorFactory;
import com.mrmccormick.hydra.mqtt.ignition.TagChangeEvent;
import com.mrmccormick.hydra.mqtt.implementation.ChunkBuffer;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TagEngineConnector extends ActorBase implements IConnector, IRunnable, ITagChangeSubscriber, PropertyModelContributor {

    public TagEngineConnector(@NotNull String connectionName,
                              @NotNull String name,
                              @NotNull GatewayContext gatewayContext,
                              @NotNull String tagProviderName,
                              boolean publishPropertiesEnabled,
                              @NotNull IRunnerBuilder runnerBuilder) {
        super(name);

        //noinspection ConstantValue
        if (connectionName == null)
            throw new IllegalArgumentException("connectionName can not be null");

        //noinspection ConstantValue
        if (gatewayContext == null)
            throw new IllegalArgumentException("gatewayContext can not be null");
        _tagManager = gatewayContext.getTagManager();

        //noinspection ConstantValue
        if (tagProviderName == null)
            throw new IllegalArgumentException("tagProviderName can not be null");
        _tagProviderName = tagProviderName;
        _tagProvider = _tagManager.getTagProvider(tagProviderName);

        _publishPropertiesEnabled = publishPropertiesEnabled;

        //noinspection ConstantValue
        if (runnerBuilder == null)
            throw new IllegalArgumentException("runnerBuilder can not be null");
        _runner = runnerBuilder.build(this);

        var logName = connectionName + "." + getClass().getSimpleName() + "." + name;
        _logger = GatewayHook.getLogger(logName);
        _outboundBuffer = new ChunkBuffer<>(logName + ".Buffer");
    }

    @Override
    public void abortConnecting() {
        // Can't abort, do nothing.
    }

    @Override
    public boolean clear() {
        _outboundBufferClearFlag = true;
        boolean success = removeAllTags();

        for (int i = 0; i < 1000; i++) {
            if (_outboundBufferClearAcknowledged) {
                _outboundBufferClearFlag = false;
                _outboundBufferClearAcknowledged = false;
                return success;
            }

            try {
                Thread.sleep(10);
            } catch (Exception e) {
                _logger.warn("Failed to sleep while waiting for buffer to clear: " + e, e);
                break;
            }
        }

        _outboundBufferClearFlag = false;
        _outboundBufferClearAcknowledged = false;
        return false;
    }

    @Override
    public void configure(@Nullable MutableConfigurationPropertyModel mcpm) {
        if (mcpm == null)
            return;

        if (_publishPropertiesEnabled) {
            try {
                mcpm.addProperties(_propertyRoutingPublishTopicOverride);
                mcpm.addProperties(_propertyRoutingPublishEnabled);
                //noinspection unchecked
                mcpm.registerAllowedValues(_propertyRoutingPublishEnabled, ImmutableList.of(true, false));
            } catch (Exception e) {
                _logger.error("Could not configure routing properties: " + e, e);
            }
        }
    }

    @Override
    public void connect() throws Exception {
        if (_isConnecting) {
            _logger.debug("Connect requested but already connecting. Ignoring connect request.");
            return;
        }

        if (_isConnected) {
            _logger.debug("Connect requested but already connected. Ignoring connect request.");
            return;
        }

        // Load TagEngineConnector.properties values with 'hydramqtt.' prefix
        BundleUtil.get().addBundle("hydramqtt", this.getClass(), "TagEngineConnector");

        // Register as a class that contributes properties
        _tagManager.getConfigManager().registerTagPropertyContributor(this);

        // Initialize properties with values from TagEngineConnector.properties
        initializeProperty(_propertyRoutingPublishEnabled,
                EventProperty.RoutingPublishEnabled.name(),
                Boolean.class, true);
        initializeProperty(_propertyRoutingPublishTopicOverride,
                EventProperty.RoutingPublishTopicOverride.name(),
                String.class, "");

        if (!TagChangeActorFactory.singleton.tagProviderWasInitialized(_tagProvider))
            TagChangeActorFactory.singleton.initializeTagProvider(_tagProvider);
        TagChangeActorFactory.singleton.subscribe(this, _tagProvider);

        _isConnecting = false;
        _isConnected = true;
    }

    @Override
    public void disconnect() {
        if (!_isConnected) {
            _logger.error("Can not disconnect because a connection has not been initialized.");
            return;
        }
        _isDisconnecting = true;

        TagChangeActorFactory.singleton.unSubscribe(this, _tagProvider);

        BundleUtil.get().removeBundle("hydramqtt");

        _tagManager.getConfigManager().unregisterTagPropertyContributor(this);

        _isDisconnecting = false;
        _isConnected = false;
    }

    @Override
    public IRunner getRunner() {
        return _runner;
    }

    @Override
    public boolean isConnected() {
        return _isConnected;
    }

    @Override
    public boolean isConnecting() {
        return _isConnecting;
    }

    @Override
    public boolean isDisconnecting() {
        return _isDisconnecting;
    }

    @Override
    public boolean isEnabled() {
        return _enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public void legacyTagChanged(TagChangeEvent tagChangeEvent) {
        if (!_enabled) {
            return;
        }

        if (tagChangeEvent == null) {
            _logger.warn("legacyTagChanged() provided a null tagChangeEvent. Rejecting tagChangedEvent");
            return;
        }

        var tagPath = tagChangeEvent.tagPath;
        Map<String, Object> properties = new HashMap<>();
        List<TagConfigurationModel> tagConfigurationModelsList;
        try {
            tagConfigurationModelsList = _tagProvider
                    .getTagConfigsAsync(Collections.singletonList(tagPath), false, true)
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            _logger.error("legacyTagChanged() could not get tag configuration models for " +
                    tagPath.toStringFull() + ": " + e.getMessage(), e);
            return;
        }

        if (tagConfigurationModelsList == null) {
            _logger.error("legacyTagChanged() received null configuration models list for " +
                    tagPath.toStringFull());
            return;
        }

        if (tagConfigurationModelsList.isEmpty()) {
            _logger.error("legacyTagChanged() received empty configuration models list for " +
                    tagPath.toStringFull());
            return;
        }

        if (tagConfigurationModelsList.size() != 1) {
            _logger.warn("legacyTagChanged() received " + tagConfigurationModelsList.size() +
                    " configuration models list for " + tagPath.toStringFull() + " when only one" +
                    " was requested");
        }

        TagConfigurationModel tagConfig = tagConfigurationModelsList.get(0);

        if (tagConfig.get(WellKnownTagProps.DataType) == DataType.Document) {
            // The tag is a UDT, don't publish
            // This should have been filtered out by not subscribing to Document changes
            _logger.warn("legacyTagChanged() Received a Document. Rejecting tagChangedEvent");
            return;
        }

        if (tagConfig.getType() == TagObjectType.Folder) {
            // The tag is folder, don't publish
            // This should have been filtered out by not subscribing to Document changes
            _logger.warn("legacyTagChanged() Received a Folder. Rejecting tagChangedEvent");
            return;
        }

        Boolean publishEnabled = null;
        String publishTopicOverride = null;
        var tagProperties = tagConfig.getTagProperties();
        if (_publishPropertiesEnabled) {
            try {
                //noinspection unchecked
                publishEnabled = (Boolean) tagProperties.get(_propertyRoutingPublishEnabled);
            } catch (Exception e) {
                _logger.warn("PublishEnabled property returned non-Boolean value: " + e.getMessage(), e);
            }

            try {
                //noinspection unchecked
                publishTopicOverride = (String) tagProperties.get(_propertyRoutingPublishTopicOverride);
            } catch (Exception e) {
                _logger.warn("PublishTopic property returned non-String value: " + e.getMessage(), e);
            }
        }

        if (publishEnabled != null && !publishEnabled) {
            return;
        }

        if (publishEnabled != null)
            properties.put(EventProperty.RoutingPublishEnabled.name(), publishEnabled);
        if (publishTopicOverride != null)
            properties.put(EventProperty.RoutingPublishTopicOverride.name(), publishTopicOverride);
        properties.put(EventProperty.Documentation.name(), tagProperties.get(WellKnownTagProps.Documentation));
        properties.put(EventProperty.EngineeringUnits.name(), tagProperties.get(WellKnownTagProps.EngUnit));

        var timestamp = tagChangeEvent.value.getTimestamp();
        var value = tagChangeEvent.value.getValue();

        Event event = new Event(tagPath.toStringPartial(), timestamp, value, properties);
        for (var handler : _subscribers) {
            handler.receive(event);
        }
    }

    @Override
    public void onTagPathChange(@NotNull TagPath oldPath, @NotNull TagPath newPath) {

    }

    public void onTagValueChange(@NotNull TagChangeEvent tagChangeEvent) {
        _outboundBuffer.addToNextChunk(tagChangeEvent);
    }

    @Override
    public void receive(@NotNull Event event) {
        legacyWriteTag(event);
    }

    @Override
    public void run() {
        int i = -1;
        do {
            i++;

            if (_outboundBuffer.currentChunkIsEmpty())
                _outboundBuffer.loadNextChunk(i);

            while (!_outboundBuffer.currentChunkIsEmpty()) {
                TagChangeEvent tagChangedEvent = _outboundBuffer.currentChunkDequeue();

                if (_outboundBufferClearFlag)
                    break;

                legacyTagChanged(tagChangedEvent);
            }

            if (_outboundBufferClearFlag) {
                _outboundBuffer.clear();
                _outboundBufferClearAcknowledged = true;
                return;
            }

        } while (!_outboundBuffer.nextChunkIsEmpty());
    }

    private boolean _enabled = true;
    private boolean _isConnected = false;
    private boolean _isConnecting = false;
    private boolean _isDisconnecting = false;
    private final Logger _logger;
    private final @NotNull ChunkBuffer<TagChangeEvent> _outboundBuffer;
    private volatile boolean _outboundBufferClearAcknowledged = false;
    private volatile boolean _outboundBufferClearFlag = false;
    private static final BasicConfigurationProperty _propertyRoutingPublishEnabled =
            new BasicConfigurationProperty<Boolean>();
    private static final BasicConfigurationProperty _propertyRoutingPublishTopicOverride =
            new BasicConfigurationProperty<String>();
    private final boolean _publishPropertiesEnabled;
    private final IRunner _runner;
    private final GatewayTagManager _tagManager;
    private final TagProvider _tagProvider;
    private final String _tagProviderName;

    private List<NodeDescription> browseFolder(@NotNull TagPath tagPath) throws Exception {
        //noinspection ConstantValue
        if (tagPath == null)
            throw new RuntimeException("tagPath can not be null");

        var results = _tagProvider.browseAsync(tagPath, BrowseFilter.NONE).get();

        if (results.getResultQuality().isNotGood())
            throw new Exception("Browse folder " + tagPath.toStringFull() + " node descriptions returned bad result.");

        var nodeDescriptions = results.getResults();
        if (nodeDescriptions == null)
            throw new Exception("Could not browse folder " + tagPath.toStringFull() + " node descriptions.");

        return nodeDescriptions.stream().toList();
    }

    private List<NodeDescription> browseRoot() throws Exception {
        return browseFolder(TagPathParser.parse(""));
    }

    private static DataType getDataType(Object object) throws Exception {
        if (object instanceof Boolean) return DataType.Boolean;
        if (object instanceof String) return DataType.String;
        if (object instanceof Double) return DataType.Float8;
        if (object instanceof Float) return DataType.Float4;
        if (object instanceof Long) return DataType.Int8;
        if (object instanceof Integer) return DataType.Int4;
        if (object instanceof Short) return DataType.Int2;
        if (object instanceof Byte) return DataType.Int1;
        if (object instanceof Date) return DataType.DateTime;
        if (object instanceof Boolean[]) return DataType.BooleanArray;
        if (object instanceof String[]) return DataType.StringArray;
        if (object instanceof Double[]) return DataType.Float8Array;
        if (object instanceof Float[]) return DataType.Float4Array;
        if (object instanceof Long[]) return DataType.Int8Array;

        // Ensure that the data type will always be big enough
        if (object instanceof Integer[]) return DataType.Int8Array;
        //if (object instanceof Integer[]) return DataType.Int4Array;

        if (object instanceof Short[]) return DataType.Int2Array;
        if (object instanceof Byte[]) return DataType.Int1Array;
        if (object instanceof Date[]) return DataType.DateTimeArray;
        if (object instanceof Document) return DataType.Document;
        if (object instanceof BasicDataset) return DataType.DataSet;
        throw new Exception("Data type is not convertible to ignition tag data type." +
                " Name: " + object.getClass().getName() + " , SimpleName: " + object.getClass().getSimpleName());
    }

    private static void initializeProperty(BasicConfigurationProperty property, String name, Class clazz, Object defaultValue) {
        property.setName(name);
        //noinspection unchecked
        property.setClazz(clazz);
        property.setDefaultValue(defaultValue);
        property.setDisplayName(new LocalizedString("hydramqtt.TagProperties.HydraMQTT" + name + ".Name"));
        property.setCategory(new LocalizedString("hydramqtt.TagProperties.HydraMQTTRoutingPublish.Category"));
        property.setDescription(new LocalizedString("hydramqtt.TagProperties.HydraMQTT" + name + ".Description"));
    }

    private void legacyCreateTagParentFolders(String tagPath) throws Exception {
        List<TagConfiguration> createConfigs = new ArrayList<>();
        TagPath path = TagPathParser.parse(_tagProviderName, tagPath);

        // Create a list of parent folders
        List<TagPath> parentPaths = new ArrayList<>();
        TagPath parentPath = path;
        while (true) {
            parentPath = parentPath.getParentPath();
            var parentPathPartial = parentPath.toStringPartial();
            if (Objects.equals(parentPathPartial, ""))
                break;
            parentPaths.add(parentPath);
        }

        if (!parentPaths.isEmpty()) {
            // Check whether parent folders exist
            List<TagConfigurationModel> parentModels = _tagProvider
                    .getTagConfigsAsync(parentPaths, false, true)
                    .get(30, TimeUnit.SECONDS);
            // Reverse order so tree is traversed top down
            Collections.reverse(parentModels);
            for (var parentModel : parentModels) {
                if (parentModel.getType() == TagObjectType.Unknown) {
                    // The parent folder does not exist, add a creation config for it
                    TagConfiguration folderConfig = BasicTagConfiguration.createNew(parentModel.getPath());
                    folderConfig.setType(TagObjectType.Folder);
                    createConfigs.add(folderConfig);
                }
            }
        }

        List<QualityCode> results = _tagProvider
                .saveTagConfigsAsync(createConfigs, CollisionPolicy.Abort)
                .get(30, TimeUnit.SECONDS);
        for (int i = 0; i < results.size(); i++) {
            QualityCode result = results.get(i);
            if (result.isNotGood()) {
                _logger.error("Could not create tag: " + parentPaths.get(i).toStringFull());
                throw new Exception("Could not create tag: " + parentPaths.get(i).toStringFull());
            }
        }
    }

    private void legacyWriteTag(@NotNull Event event) {
        //noinspection ConstantValue
        if (event == null)
            throw new IllegalArgumentException("event can not be null");

        String tagPathString = event.path;
        Date timestamp = event.timestamp;
        Object value = event.value;

        if (!_enabled) {
            return;
        }

        //noinspection ConstantValue
        if (tagPathString == null) {
            _logger.warn("writeTag() provided a null tagPathString. Rejecting writeTag");
            return;
        }

        //noinspection ConstantValue
        if (timestamp == null) {
            _logger.warn("writeTag() provided a null timestamp. Rejecting writeTag");
            return;
        }

        TagPath tagPath;
        try {
            tagPath = TagPathParser.parse(_tagProviderName, tagPathString);
        } catch (Exception e) {
            _logger.error("writeTag() could not get tagPath for " + tagPathString + ": " +
                    e.getMessage(), e);
            return;
        }

        List<TagConfigurationModel> tagConfigurationModelsList;
        try {
            tagConfigurationModelsList = _tagProvider
                    .getTagConfigsAsync(Collections.singletonList(tagPath), false, true)
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            _logger.error("writeTag() could not get tag configuration models for " +
                    tagPath.toStringFull() + ": " + e.getMessage(), e);
            return;
        }

        if (tagConfigurationModelsList == null) {
            _logger.error("writeTag() received null configuration models list for " +
                    tagPath.toStringFull());
            return;
        }

        if (tagConfigurationModelsList.isEmpty()) {
            _logger.error("writeTag() received empty configuration models list for " +
                    tagPath.toStringFull());
            return;
        }

        if (tagConfigurationModelsList.size() > 1) {
            _logger.warn("writeTag() received " + tagConfigurationModelsList.size() +
                    " configuration models list for " + tagPath.toStringFull() + " when only one" +
                    " was requested");
        }

        TagConfigurationModel tagConfig = tagConfigurationModelsList.get(0);

        boolean needToUpdateTagConfig = false;

        // Check whether the tag already exists
        if (tagConfig.getType() == TagObjectType.Unknown) {
            // Tag does not exist, so create it
            needToUpdateTagConfig = true;
            try {
                legacyCreateTagParentFolders(tagPathString);
            } catch (Exception e) {
                _logger.error("writeTag() could not create parent folders for " + tagPath.toStringFull() +
                        ": " + e.getMessage(), e);
                return;
            }
        }

        DataType oldTagDataType = tagConfig.get(WellKnownTagProps.DataType);
        DataType newTagDataType;
        try {
            newTagDataType = getDataType(value);
        } catch (Exception e) {
            _logger.error("writeTag() could not get data type for new value of tag " +
                    tagPath.toStringFull() + ": " + e.getMessage(), e);
            return;
        }

        // Check whether the tag data type has changed
        if (oldTagDataType != newTagDataType) {
            // oldTagDataType == null -> New Tag, so set type, or
            // Tag has changed type
            needToUpdateTagConfig = true;
            tagConfig.set(WellKnownTagProps.DataType, newTagDataType);
        }

        if (event.hasProperty(EventProperty.Documentation.name())) {
            tagConfig.set(WellKnownTagProps.Documentation, (String) event.getPropertyValue(EventProperty.Documentation.name()));
            needToUpdateTagConfig = true;
        }

        if (event.hasProperty(EventProperty.EngineeringUnits.name())) {
            tagConfig.set(WellKnownTagProps.EngUnit, (String) event.getPropertyValue(EventProperty.EngineeringUnits.name()));
            needToUpdateTagConfig = true;
        }

        // If the tag config has changed and needs updated, update it.
        if (needToUpdateTagConfig) {
            List<QualityCode> configQualityCodes;
            try {
                configQualityCodes = _tagProvider
                        .saveTagConfigsAsync(Collections.singletonList(tagConfig), CollisionPolicy.Abort)
                        .get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                _logger.error("writeTag() could not save updated tag configuration for " +
                        tagPath.toStringFull() + ": " + e.getMessage(), e);
                return;
            }

            if (configQualityCodes.size() > 1) {
                _logger.warn("writeTag() received " + configQualityCodes.size() + " update configuration " +
                        "quality codes for " + tagPath.toStringFull() + " when only one was requested");
            }

            QualityCode saveResult = configQualityCodes.get(0);
            if (saveResult.isNotGood()) {
                _logger.error("writeTag() could not save updated tag configuration for " +
                        tagPath.toStringFull() + ": " + saveResult.getName() + " -> " +
                        saveResult.getDiagnosticMessage());
                return;
            }
        }

        var qualifiedValue = new BasicQualifiedValue(value, QualityCode.Good, timestamp);

        List<QualityCode> writeQualityCodes;
        try {
            writeQualityCodes = _tagProvider.writeAsync(Collections.singletonList(tagPath),
                            Collections.singletonList(qualifiedValue),
                            SecurityContext.emptyContext())
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            _logger.error("writeTag() could not execute write for " + tagPath.toStringFull() +
                    ": " + e.getMessage(), e);
            return;
        }

        if (writeQualityCodes.size() > 1) {
            _logger.warn("writeTag() received " + writeQualityCodes.size() + " write result " +
                    "quality codes for " + tagPath.toStringFull() + " when only one was requested");
        }

        QualityCode writeResult = writeQualityCodes.get(0);
        if (writeResult.isNotGood()) {
            _logger.error("writeTag() write value was not successful for tag " +
                    tagPath.toStringFull() + ": " + writeResult.getName() + " -> " +
                    writeResult.getDiagnosticMessage());
        }
    }

    private boolean removeAllTags() {
        Collection<NodeDescription> rootTags;
        try {
            rootTags = browseRoot();
            //rootTags = browseRootRecursive();
        } catch (Exception e) {
            _logger.error("Failed to recursively browse tags of " + getName() + ": " + e, e);
            return false;
        }

        List<TagPath> tagPaths = new ArrayList<>();
        for (var rootTag : rootTags) {
            tagPaths.add(rootTag.getFullPath());
        }

        try {
            _tagProvider.removeTagConfigsAsync(tagPaths).get();
        } catch (Exception e) {
            _logger.error("Failed to remove all tags from " + getName() + ": " + e, e);
        }

        TagProviderInformation info;
        try {
            info = _tagProvider.getStatusInformation().get();
        } catch (Exception e) {
            _logger.error("Failed to get status information about " + getName() + ": " + e, e);
            return false;
        }

        int tagCount = info.getTagCount();
        // One folder always left behind: '/_types_'
        return tagCount <= 1;
    }
}
