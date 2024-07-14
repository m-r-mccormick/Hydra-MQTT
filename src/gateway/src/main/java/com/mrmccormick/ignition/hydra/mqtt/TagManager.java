package com.mrmccormick.ignition.hydra.mqtt;

import com.mrmccormick.ignition.hydra.mqtt.data.DataEvent;
import com.mrmccormick.ignition.hydra.mqtt.data.DataEventSubscriber;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.inductiveautomation.ignition.common.model.values.BasicQualifiedValue;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.sqltags.model.types.DataType;
import com.inductiveautomation.ignition.common.tags.config.BasicTagConfiguration;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfigurationModel;
import com.inductiveautomation.ignition.common.tags.config.properties.WellKnownTagProps;
import com.inductiveautomation.ignition.common.tags.config.types.TagObjectType;
import com.inductiveautomation.ignition.common.tags.model.SecurityContext;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.model.GatewayTagManager;
import org.apache.log4j.Logger;

import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;

public class TagManager implements DataEventSubscriber {

    private final GatewayContext _context;
    private final GatewayTagManager _tagManager;
    private TagProvider _tagProvider;
    private final String _tagProviderName;
    private final Logger _logger = GatewayHook.GetLogger(getClass());

    public TagManager(GatewayContext gatewayContext, String tagProviderName) throws Exception {
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
        _logger.info("Tag manager started.");
    }

    public void Shutdown() throws Exception {
        _logger.info("Tag manager stopped.");
    }

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

    @Override
    public void HandleDataEvent(DataEvent dataEvent) {
        try {
            EditTag(dataEvent.Path, dataEvent.Timestamp, dataEvent.Value);
        } catch (Exception e) {
            // Do Nothing
        }
    }
}
