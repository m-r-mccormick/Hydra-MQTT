package com.mrmccormick.ignition.hydra.mqtt;

import com.google.common.collect.ImmutableList;
import com.inductiveautomation.ignition.common.BundleUtil;
import com.inductiveautomation.ignition.common.config.*;
import com.inductiveautomation.ignition.common.i18n.LocalizedString;
import com.inductiveautomation.ignition.gateway.web.models.ConfigCategory;
import com.mrmccormick.ignition.hydra.mqtt.data.DataEvent;
import com.mrmccormick.ignition.hydra.mqtt.data.IDataEventSubscriber;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
//import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;
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
import com.inductiveautomation.ignition.common.tags.model.event.InvalidListenerException;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeEvent;
import com.inductiveautomation.ignition.common.tags.model.event.TagChangeListener;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import com.inductiveautomation.ignition.gateway.tags.model.TagStructureEvent;
import com.inductiveautomation.ignition.gateway.tags.model.TagStructureListener;
import org.apache.log4j.Logger;

import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;

public class TagManager implements TagChangeListener, TagStructureListener, IDataEventSubscriber {



    private final GatewayContext _context;
    private final GatewayTagManager _tagManager;
    private TagProvider _tagProvider;
    private final String _tagProviderName;
    private final Logger _logger = GatewayHook.GetLogger(getClass());

    public List<IDataEventSubscriber> DataEventSubscribers = new ArrayList<>();

    public TagManager(GatewayContext gatewayContext, String tagProviderName) {
        _context = gatewayContext;
        _tagManager = _context.getTagManager();
        _tagProviderName = tagProviderName;
    }

    public void Startup() throws Exception {
        _tagProvider = _tagManager.getTagProvider(_tagProviderName);
        if (_tagProvider == null) {
            _logger.fatal("Could not find tag provider: " + _tagProviderName);
            throw new RuntimeException("Could not find tag provider: " + _tagProviderName);
        }
        _tagManager.addTagStructureListener(_tagProviderName, this);
        for (var tag : BrowseTags()) {
            _tagManager.subscribeAsync(tag.getPath(), this);
        }
    }

    public void Shutdown() throws Exception {
        _tagManager.removeTagStructureListener(_tagProviderName, this);
        for (var tag : BrowseTags()) {
            _tagManager.unsubscribeAsync(tag.getPath(), this);
        }
    }

    private void InitializeProperty(BasicConfigurationProperty property, String name, Class clazz, Object defaultValue) {
        property.setName(name);
        property.setClazz(clazz);
        property.setDefaultValue(defaultValue);
        property.setDisplayName(new LocalizedString("hydramqtt.TagProperties." + name + ".Name"));
        property.setCategory(new LocalizedString("hydramqtt.TagProperties." + name + ".Category"));
        property.setDescription(new LocalizedString("hydramqtt.TagProperties." + name + ".Description"));
    }

    public static Property<Date> DataEventTimestamp = new BasicDescriptiveProperty(
            "DataEventTimestamp", // Name
            WellKnownTagProps.propertyKey("DataEventTimestamp"), // Display Name Key
            WellKnownTagProps.categoryKey("value"), // Category Key
            WellKnownTagProps.descriptionKey("DataEventTimestamp"), // Description Key
            Date.class, // Type
            (Object)null // Default Value
    );

    public void EditTag(String tagPath, Date timestamp, Object value) throws Exception {
        TagPath path = TagPathParser.parse(_tagProviderName, tagPath);

        // Check whether tag exists
        TagConfigurationModel tagConfig = _tagProvider
                .getTagConfigsAsync(Collections.singletonList(path), false, true)
                .get(30, TimeUnit.SECONDS)
                .get(0);
        DataType oldTagDataType = tagConfig.get(WellKnownTagProps.DataType);
        DataType newTagDataType = GetDataType(value);
        boolean saveTagConfig = false;
        if (tagConfig.getType() == TagObjectType.Unknown) {
            // Tag does not exist, so create it
            saveTagConfig = true;
            CreateTagParentFolders(tagPath, timestamp, value);
        }
        if (oldTagDataType != newTagDataType) {
            // oldTagDataType == null -> New Tag, so set type, or
            // Tag has changed type
            saveTagConfig = true;
            tagConfig.set(WellKnownTagProps.DataType, newTagDataType);
        }
        if (saveTagConfig) {

            QualityCode saveResult = _tagProvider
                    .saveTagConfigsAsync(Collections.singletonList(tagConfig), CollisionPolicy.Abort)
                    .get(30, TimeUnit.SECONDS)
                    .get(0);
            if (saveResult.isNotGood()) {
                _logger.error("Could not edit tag: " + path.toStringFull());
                throw new Exception("Could not edit tag: " + path.toStringFull());
            }
        }

        var qualifiedValue = new BasicQualifiedValue(value, QualityCode.Good, timestamp);

        QualityCode writeResult =
                _tagProvider.writeAsync(Collections.singletonList(path),
                                        Collections.singletonList(qualifiedValue),
                                        SecurityContext.emptyContext())
                        .get(30, TimeUnit.SECONDS)
                        .get(0);
        if (writeResult.isNotGood()) {
            _logger.error("Could not write value to tag: " + path.toStringFull());
            throw new Exception("Could not write value to tag: " + path.toStringFull());
        }
    }

    private void CreateTagParentFolders(String tagPath, Date timestamp, Object initialValue) throws Exception {
        List<TagConfiguration> createConfigs = new ArrayList<>();
        TagPath path = TagPathParser.parse(_tagProviderName, tagPath);

        // Create a list of parent folders
        List<TagPath> parentPaths = new ArrayList<>();
        TagPath parentPath = path;
        String root_path = "[" + _tagProviderName + "]";
        while (true)
        {
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

    private DataType GetDataType(Object object) throws Exception {
        if (object instanceof Boolean) return DataType.Boolean;
        if (object instanceof String) return DataType.String;
        if (object instanceof Double) return DataType.Float8;
        if (object instanceof Float) return DataType.Float4;
        if (object instanceof Long) return DataType.Int8;
        if (object instanceof Integer) return DataType.Int4;
        if (object instanceof Short) return DataType.Int2;
        if (object instanceof Byte) return DataType.Int1;
        if (object instanceof Date) return DataType.DateTime;
        throw new Exception("Invalid Type: " + object.getClass());
    }

    public List<TagConfiguration> BrowseTags() throws Exception {
        List<TagConfiguration> tagConfigs = new ArrayList<>();
        TagPath path = TagPathParser.parseSafe("");
        BrowseFolderRecursive(path, tagConfigs);
        List<TagConfiguration> removeConfigs = new ArrayList<>();

        return tagConfigs;
    }

    private void BrowseFolderRecursive(TagPath path, List<TagConfiguration> tagList) throws Exception {
        var results = _tagProvider.browseAsync(path, BrowseFilter.NONE).get();

        if (results.getResultQuality().isNotGood()){
            throw new Exception("BrowseFolderRecursive Bad Quality Results");
        }

        for (NodeDescription nodeDescription : results.getResults()) {
            TagPath childPath = path.getChildPath(nodeDescription.getName());
            List<TagPath> childPaths = new ArrayList<>();
            childPaths.add(childPath);
            var tagConfigurationModels = _tagProvider.getTagConfigsAsync(childPaths, true, true).get();
            tagList.addAll(tagConfigurationModels);

            // Don't include Documents (such as UDTs)
            if (nodeDescription.hasChildren() && nodeDescription.getDataType() != DataType.Document) {
                BrowseFolderRecursive(childPath, tagList);
            }
        }
    }

    @Override
    public void tagChanged(TagChangeEvent tagChangeEvent) {
        var tagPath = tagChangeEvent.getTagPath();
        var tagValue = tagChangeEvent.getValue();
        var timestamp = tagValue.getTimestamp();
        var value = tagValue.getValue();

        TagConfigurationModel tagConfig;
        try {
            // Check whether tag exists
            tagConfig = _tagProvider
                    .getTagConfigsAsync(Collections.singletonList(tagPath), false, true)
                    .get(30, TimeUnit.SECONDS)
                    .get(0);
        } catch (Exception e) {
            _logger.error("Could not get tag configuration: " + tagPath.toStringFull());
            return;
        }
        if (tagConfig.get(WellKnownTagProps.DataType) == DataType.Document)
            // The tag is a UDT, don't publish
            return;
        if (tagConfig.getType() == TagObjectType.Folder)
            // The tag is folder, don't publish
            return;
        if (tagChangeEvent.isInitial())
            // Tag was just created, don't publish
            return;

        var publishEnabled = true;
        String publishTopic = null;

        if (!publishEnabled)
            return;

        DataEvent event = new DataEvent(tagPath.toStringPartial(), timestamp, value, publishTopic);
        for (var handler : DataEventSubscribers) {
            handler.HandleDataEvent(event);
        }
    }

    @Override
    public void tagStructureChanged(TagStructureEvent tagStructureEvent) {
        var addedTags = tagStructureEvent.getAddedTags();
        var removedTags = tagStructureEvent.getRemovedTagsInfo();
        for (var addedTag : addedTags) {
            if (addedTag.getDataType() == DataType.Document)
                // Tag is a UDT, so don't subscribe
                continue;

            if (addedTag.getObjectType() == TagObjectType.Folder)
                // Tag is a folder, so don't subscribe
                continue;

            try {
                _tagManager.subscribeAsync(addedTag.getFullPath(), this);
                _logger.debug("Subscribed to tag changes: " + addedTag.getFullPath());
            } catch (Exception e) {
                _logger.fatal("Could not subscribe to tag changes: " + addedTag.getFullPath(), e);
            }
        }
        for (var removedTag : removedTags) {
            _tagManager.unsubscribeAsync(removedTag.getFullPath(), this);
        }
    }

    @Override
    public void HandleDataEvent(DataEvent dataEvent) {
        try {
            EditTag(dataEvent.Path, dataEvent.Timestamp, dataEvent.Value);
        } catch (Exception e) {
        }
    }
}
