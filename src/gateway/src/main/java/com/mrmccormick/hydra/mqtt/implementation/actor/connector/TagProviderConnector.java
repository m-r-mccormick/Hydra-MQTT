package com.mrmccormick.hydra.mqtt.implementation.actor.connector;

import com.inductiveautomation.ignition.common.BasicDataset;
import com.inductiveautomation.ignition.common.config.*;
import com.inductiveautomation.ignition.common.document.Document;
import com.inductiveautomation.ignition.common.i18n.LocalizedString;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import com.mrmccormick.hydra.mqtt.domain.Event;
import com.mrmccormick.hydra.mqtt.domain.actor.ActorBase;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.IConnector;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.model.values.BasicQualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.BasicTagConfiguration;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.model.SecurityContext;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeEvent;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeListener;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import com.inductiveautomation.ignition.gateway.tags.model.TagStructureEvent;
import com.inductiveautomation.ignition.gateway.tags.model.TagStructureListener;
import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class TagProviderConnector extends ActorBase implements IConnector, TagChangeListener, TagStructureListener {

    public TagProviderConnector(@NotNull String connectionName,
                                @NotNull String name,
                                @NotNull GatewayContext context,
                                @NotNull String tagProviderName) {
        super(name);

        //noinspection ConstantValue
        if (connectionName == null)
            throw new IllegalArgumentException("connectionName cannot be null");
        _logger = GatewayHook.getLogger(connectionName + "." + getClass().getSimpleName() + "." + name);

        //noinspection ConstantValue
        if (context == null)
            throw new IllegalArgumentException("context cannot be null");
        _tagManager = context.getTagManager();

        //noinspection ConstantValue
        if (tagProviderName == null)
            throw new IllegalArgumentException("tagProviderName cannot be null");
        _tagProviderName = tagProviderName;
    }

    @Override
    public void abortConnecting() {
        // Can't abort, do nothing
    }

    public boolean clear() {
        removeAllTags();

        int total;
        try {
            total = (int) Objects.requireNonNull(getTagCounts()).get("total");
        } catch (Exception e) {
            _logger.error("Unable to get total tag count of " + _tagProviderName + " Tag Provider: " + e.getMessage(), e);
            return false;
        }
        // can't remove __types__, which is a folder, and always gets left behind.
        if (total > 1) {
            _logger.warn("Unable to clear " + _tagProviderName + " Tag Provider. Total tags: " + String.format("%,d", total));
            return false;
        }

        return true;
    }

    @Override
    public void connect() {
        if (_isConnecting) {
            _logger.debug("Connect requested but already connecting. Ignoring connect request.");
            return;
        }

        if (_isConnected) {
            _logger.debug("Connect requested but already connected. Ignoring connect request.");
            return;
        }

        _isConnecting = true;

        _tagProvider = _tagManager.getTagProvider(_tagProviderName);

        if (_tagProvider == null) {
            _logger.error("Tag provider '" + _tagProviderName + "' does not exist");
            _isConnecting = false;
            return;
        }

        // Subscribe to all tags in the tag provider
        _tagManager.addTagStructureListener(_tagProviderName, this);
        try {
            allTagsSubscribe();
        } catch (Exception e) {
            _tagManager.removeTagStructureListener(_tagProviderName, this);
            _isConnecting = false;
            _logger.error("Could not subscribe to all tags: " + e, e);
            try {
                allTagsUnsubscribe();
            } catch (Exception ex) {
                _logger.warn("Could not unsubscribe from partially-subscribed tags: " + e, e);
            }
            _isConnecting = false;
            return;
        }

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

        try {
            allTagsUnsubscribe();
        } catch (Exception e) {
            _logger.warn("Could not unsubscribe from all tags: " + e, e);
        }

        _tagManager.removeTagStructureListener(_tagProviderName, this);

        _isDisconnecting = false;
        _isConnected = false;
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
    public void receive(@NotNull Event event) {
        //noinspection ConstantValue
        if (event == null)
            throw new NullPointerException("Event cannot be null");
        writeTag(event);
    }

    @Override
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    @Override
    public void tagChanged(TagChangeEvent tagChangeEvent) {
        if (!_enabled) {
            return;
        }

        if (tagChangeEvent == null) {
            _logger.warn("tagChanged() provided a null tagChangeEvent. Rejecting tagChangedEvent");
            return;
        }

        // As soon as subscriptions are added and the tagProvider is connected, the tag provider
        //  will immediately report the status of every tag. Block it via this.
        if (tagChangeEvent.isInitial()) {
            return;
        }

        if (tagChangeEvent.getValue().getValue() == null) {
            // Tag has a null value, don't publish
            // Null value can be due to:
            //  1) The user creates a new tag in the Designer but does not assign a value, so the value is null
            //  2) A new tag config is saved (without writing value property with value in config)
            return;
        }

        var tagPath = tagChangeEvent.getTagPath();

        Map<String, Object> properties = new HashMap<>();
        List<TagConfigurationModel> tagConfigurationModelsList;
        try {
            tagConfigurationModelsList = _tagProvider
                    .getTagConfigsAsync(Collections.singletonList(tagPath), false, true)
                    .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            _logger.error("tagChanged() could not get tag configuration models for " +
                    tagPath.toStringFull() + ": " + e.getMessage(), e);
            return;
        }

        if (tagConfigurationModelsList == null) {
            _logger.error("tagChanged() received null configuration models list for " +
                    tagPath.toStringFull());
            return;
        }

        if (tagConfigurationModelsList.isEmpty()) {
            _logger.error("tagChanged() received empty configuration models list for " +
                    tagPath.toStringFull());
            return;
        }

        if (tagConfigurationModelsList.size() != 1) {
            _logger.warn("tagChanged() received " + tagConfigurationModelsList.size() +
                    " configuration models list for " + tagPath.toStringFull() + " when only one" +
                    " was requested");
        }

        TagConfigurationModel tagConfig = tagConfigurationModelsList.get(0);

        if (tagConfig.get(WellKnownTagProps.DataType) == DataType.Document) {
            // The tag is a UDT, don't publish
            // This should have been filtered out by not subscribing to Document changes
            _logger.warn("tagChanged() Received a Document. Rejecting tagChangedEvent");
            return;
        }

        if (tagConfig.getType() == TagObjectType.Folder) {
            // The tag is folder, don't publish
            // This should have been filtered out by not subscribing to Document changes
            _logger.warn("tagChanged() Received a Folder. Rejecting tagChangedEvent");
            return;
        }

        var timestamp = tagChangeEvent.getValue().getTimestamp();
        var value = tagChangeEvent.getValue().getValue();

        Event event = new Event(tagPath.toStringPartial(), timestamp, value, properties);
        for (var handler : _subscribers) {
            handler.receive(event);
        }
    }

    @Override
    public void tagStructureChanged(@Nullable TagStructureEvent tagStructureEvent) {
        if (tagStructureEvent == null)
            return;

        for (var addedTag : tagStructureEvent.getAddedTags()) {
            var tagPath = addedTag.getFullPath();
            try {
                if (getShouldNotSubscribe(addedTag.getObjectType(), addedTag.getDataType()))
                    continue;
            } catch (Exception e) {
                _logger.error("tagStructureChanged() received invalid addedTag " +
                        tagPath.toStringFull() + ": " + e.getMessage(), e);
                continue;
            }

            try {
                _tagManager.subscribeAsync(tagPath, this).get();
            } catch (Exception e) {
                _logger.error("Could not subscribe to addedTag tag " +
                        addedTag.getFullPath().toStringFull() + " changes: " + e.getMessage(), e);
            }
        }
        for (var movedTag : tagStructureEvent.getMovedTags()) {
            var oldTagPath = movedTag.getLeft();
            var newTag = movedTag.getRight();
            var newTagPath = newTag.getFullPath();
            try {
                if (getShouldNotSubscribe(newTag.getObjectType(), newTag.getDataType()))
                    continue;
            } catch (Exception e) {
                _logger.info("tagStructureChanged() movedTag oldTagPath: " + oldTagPath.toStringFull());
                _logger.info("tagStructureChanged() movedTag newTagPath: " + newTagPath.toStringFull());
                _logger.error("tagStructureChanged() received invalid movedTag newTagPath " +
                        newTagPath.toStringFull() + ": " + e.getMessage(), e);
                continue;
            }

            try {
                _tagManager.unsubscribeAsync(oldTagPath, this).get();
            } catch (Exception e) {
                _logger.error("Could not unsubscribe from movedTag oldTagPath tag " +
                        oldTagPath.toStringFull() + " changes: " + e.getMessage(), e);
            }

            try {
                _tagManager.subscribeAsync(newTagPath, this).get();
            } catch (Exception e) {
                _logger.error("Could not subscribe to movedTag newTagPath tag " +
                        newTagPath.toStringFull() + " changes: " + e.getMessage(), e);
            }
        }
        for (var removedTag : tagStructureEvent.getRemovedTagsInfo()) {
            try {
                if (getShouldNotSubscribe(removedTag.getObjectType(), removedTag.getDataType()))
                    continue;
            } catch (Exception e) {
                _logger.error("tagStructureChanged() received invalid removedTag: " +
                        removedTag.getFullPath().toStringFull() + ": " + e.getMessage(), e);
                continue;
            }

            try {
                _tagManager.unsubscribeAsync(removedTag.getFullPath(), this).get();
            } catch (Exception e) {
                _logger.warn("Could not unsubscribe from removedTag tag " +
                        removedTag.getFullPath().toStringFull() + " changes: " + e.getMessage(), e);
            }
        }
    }

    private boolean _enabled = true;
    private boolean _isConnected = false;
    private boolean _isConnecting = false;
    private boolean _isDisconnecting = false;
    private final Logger _logger;
    private final GatewayTagManager _tagManager;
    private TagProvider _tagProvider;
    private final String _tagProviderName;

    private void allTagsSubscribe() throws Exception {
        //noinspection DuplicatedCode
        List<TagPath> subPaths = new ArrayList<>();
        List<TagChangeListener> subListeners = new ArrayList<>();
        Collection<NodeDescription> recursiveRootNodeDescriptions;
        try {
            recursiveRootNodeDescriptions = browseRootNodeDescriptionsRecursive();
        } catch (Exception e) {
            throw new RuntimeException(_tagProviderName + "Failed to recursively browse all Tags: " + e, e);
        }

        for (var description : recursiveRootNodeDescriptions) {
            if (description == null) {
                _logger.warn(_tagProviderName + " returned a null node description. " +
                        "Skipping creating tag change subscription to existing tag with null node description.");
                continue;
            }
            if (getShouldNotSubscribe(description.getObjectType(), description.getDataType()))
                continue;
            var tagPath = description.getFullPath();
            if (tagPath == null) {
                _logger.warn(_tagProviderName + " returned a null tag path. " +
                        "Skipping creating tag change subscription to existing tag with null tag path.");
                continue;
            }
            subPaths.add(tagPath);
            subListeners.add(this);
        }

        try {
            _tagManager.subscribeAsync(subPaths, subListeners).get();
        } catch (Exception e) {
            throw new Exception(_tagProviderName + " could not subscribe to all existing tags in '" +
                    _tagProviderName + "' tag provider: " + e.getMessage(), e);
        }
    }

    private void allTagsUnsubscribe() throws Exception {
        List<TagPath> unSubPaths = new ArrayList<>();
        List<TagChangeListener> unSubListeners = new ArrayList<>();
        Collection<NodeDescription> recursiveRootNodeDescriptions;
        try {
            recursiveRootNodeDescriptions = browseRootNodeDescriptionsRecursive();
        } catch (Exception e) {
            throw new RuntimeException(_tagProviderName + "Failed to recursively browse all Tags: " + e, e);
        }

        for (var description : recursiveRootNodeDescriptions) {
            if (description == null) {
                _logger.warn(_tagProviderName + " returned a null node description. " +
                        "Skipping creating tag change unsubscription to existing tag with null node description.");
                continue;
            }
            if (getShouldNotSubscribe(description.getObjectType(), description.getDataType()))
                continue;
            var tagPath = description.getFullPath();
            if (tagPath == null) {
                _logger.warn(_tagProviderName + " returned a null tag path. " +
                        "Skipping creating tag change unsubscription to existing tag with null tag path.");
                continue;
            }
            unSubPaths.add(tagPath);
            unSubListeners.add(this);
        }

        try {
            _tagManager.unsubscribeAsync(unSubPaths, unSubListeners).get();
        } catch (Exception e) {
            throw new Exception(_tagProviderName + " could not unsubscribe from all existing tags in '" +
                    _tagProviderName + "' tag provider: " + e.getMessage(), e);
        }
    }

    private List<NodeDescription> browseFolderNodeDescriptions(TagPath tagPath) throws Exception {
        if (tagPath == null)
            throw new Exception("tagPath can not be null");

        var results = _tagProvider.browseAsync(tagPath, BrowseFilter.NONE).get();

        if (results.getResultQuality().isNotGood()) {
            _logger.error("Tag Provider " + _tagProvider + " returned not good result during BrowseFolder.");
            throw new Exception("Tag Provider " + _tagProvider + " returned not good result during BrowseFolder.");
        }

        var nodeDescriptions = results.getResults();
        if (nodeDescriptions == null) {
            _logger.error("Tag Provider " + _tagProvider + " returned null results during BrowseFolder.");
            throw new Exception("Tag Provider " + _tagProvider + " returned null results during BrowseFolder.");
        }

        return nodeDescriptions.stream().toList();
    }

    private void browseFolderNodeDescriptionsRecursive(@NotNull TagPath tagPath,
                                                       @NotNull List<NodeDescription> descriptionList
    ) throws Exception {
        //noinspection ConstantValue,DuplicatedCode
        if (tagPath == null)
            throw new Exception("tagPath can not be null");
        //noinspection ConstantValue
        if (descriptionList == null)
            throw new Exception("descriptionList can not be null");

        for (var nodeDescription : browseFolderNodeDescriptions(tagPath)) {
            var childPath = nodeDescription.getFullPath();

            if (!nodeDescription.hasChildren())
                continue;

            // This occurs for UDTs
            if (childPath == null)
                continue;

            browseFolderNodeDescriptionsRecursive(childPath, descriptionList);
        }
    }

    private @NotNull Collection<NodeDescription> browseRootNodeDescriptionsRecursive() throws Exception {
        TagPath path = TagPathParser.parseSafe("");
        List<NodeDescription> nodeDescriptions = browseFolderNodeDescriptions(path);
        browseFolderNodeDescriptionsRecursive(path, nodeDescriptions);
        return nodeDescriptions;
    }

    private void createTagParentFolders(String tagPath) throws Exception {
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

    private DataType getDataType(Object object) throws Exception {
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

    private boolean getShouldNotSubscribe(@NotNull TagObjectType tagObjectType, @NotNull DataType dataType) {
        //noinspection ConstantValue
        if (tagObjectType == null)
            throw new IllegalArgumentException("tagObjectType can not be null");
        //noinspection ConstantValue
        if (dataType == null)
            throw new IllegalArgumentException("dataType can not be null");

        // Don't subscribe to folders
        return tagObjectType == TagObjectType.Folder;
    }

    private @Nullable Map<String, Object> getTagCounts() {
        Collection<NodeDescription> tags;
        try {
            tags = browseRootNodeDescriptionsRecursive();
        } catch (Exception e) {
            _logger.warn("Could not browse Sub-Only Tags in " + _tagProviderName + " Provider: " + e.getMessage());
            return null;
        }

        var unknown = 0;
        var property = 0;
        var node = 0;
        var folder = 0;
        var atomicTag = 0;
        var udtInstance = 0;
        var udtType = 0;
        var tagModel = 0;
        var provider = 0;
        var undefinedTagObjectType = 0;

        for (var tag : tags) {
            var type = tag.getObjectType();
            switch (type) {
                case Unknown:
                    unknown++;
                    break;
                case Property:
                    property++;
                    break;
                case Node:
                    node++;
                    break;
                case Folder:
                    folder++;
                    break;
                case AtomicTag:
                    atomicTag++;
                    break;
                case UdtInstance:
                    udtInstance++;
                    break;
                case UdtType:
                    udtType++;
                    break;
                case TagModel:
                    tagModel++;
                    break;
                case Provider:
                    provider++;
                    break;
                default:
                    undefinedTagObjectType++;
                    break;
            }
        }

        Map<String, Object> counts = new HashMap<>();
        counts.put("unknown", unknown);
        counts.put("property", property);
        counts.put("node", node);
        counts.put("folder", folder);
        counts.put("atomicTag", atomicTag);
        counts.put("udtInstance", udtInstance);
        counts.put("udtType", udtType);
        counts.put("tagModel", tagModel);
        counts.put("provider", provider);
        counts.put("undefinedTagObjectType", undefinedTagObjectType);
        counts.put("total", unknown + provider + node + folder + atomicTag + udtInstance + udtType +
                tagModel + provider + undefinedTagObjectType);

        return counts;
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

    private void removeAllTags() {
        Collection<NodeDescription> rootTags;
        try {
            rootTags = browseRootNodeDescriptionsRecursive();
        } catch (Exception e) {
            _logger.error("Failed to browse root " + getName() + " Tags: " + e, e);
            return;
        }

        List<TagPath> tagPaths = new ArrayList<>();
        for (var rootTag : rootTags) {
            tagPaths.add(rootTag.getFullPath());
        }

        try {
            _tagProvider.removeTagConfigsAsync(tagPaths).get();
        } catch (Exception e) {
            _logger.error("Failed to remove all " + getName() + " Tags: " + e, e);
        }
    }

    private void writeTag(@NotNull Event event) {
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
                createTagParentFolders(tagPathString);
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
}
