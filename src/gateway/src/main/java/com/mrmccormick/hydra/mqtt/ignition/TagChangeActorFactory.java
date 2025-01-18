package com.mrmccormick.hydra.mqtt.ignition;

import com.inductiveautomation.ignition.common.browsing.BrowseFilter;
import com.inductiveautomation.ignition.common.config.*;
import com.inductiveautomation.ignition.common.model.values.QualityCode;
import com.inductiveautomation.ignition.common.tags.browsing.NodeDescription;
import com.inductiveautomation.ignition.common.tags.config.CollisionPolicy;
import com.inductiveautomation.ignition.common.tags.config.TagConfiguration;
import com.inductiveautomation.ignition.common.tags.model.TagPath;
import com.inductiveautomation.ignition.common.tags.model.TagProvider;
import com.inductiveautomation.ignition.common.tags.paths.parser.TagPathParser;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.inductiveautomation.ignition.gateway.tags.config.TagConfigurationManager;
import com.inductiveautomation.ignition.gateway.tags.evaluation.ActorClassification;
import com.inductiveautomation.ignition.gateway.tags.evaluation.ConfigurationException;
import com.inductiveautomation.ignition.gateway.tags.evaluation.TagActor;
import com.inductiveautomation.ignition.gateway.tags.evaluation.TagActorFactory;
import com.inductiveautomation.ignition.gateway.tags.evaluation.nodes.NodeContext;
import com.mrmccormick.hydra.mqtt.GatewayHook;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TagChangeActorFactory implements TagActorFactory {

    public static TagChangeActorFactory singleton = new TagChangeActorFactory();

    @Override
    public void configureTagModel(MutableConfigurationPropertyModel model) {
        // This is for adding properties to the 'Tag Editor' dialog box
    }

    @Override
    public TagActor create(NodeContext nodeContext, PropertySet propertySet) throws ConfigurationException {
        return new TagChangeActor(_subscribers);
    }

    @Override
    public ActorClassification getActorClassification() {
        return classification;
    }

    public List<String> getInitializedTagProviderNames() {
        return new ArrayList<>(_initializedTagProviderNames);
    }

    public boolean getIsSubscribed(@NotNull ITagChangeSubscriber tagChangeSubscriber, @NotNull TagProvider tagProvider) {
        //noinspection ConstantValue
        if (tagChangeSubscriber == null)
            throw new IllegalArgumentException("tagChangeSubscriber can not be null");
        //noinspection ConstantValue
        if (tagProvider == null)
            throw new IllegalArgumentException("tagProvider can not be null");

        if (!_subscribers.containsKey(tagProvider.getName()))
            return false;
        return _subscribers.get(tagProvider.getName()).contains(tagChangeSubscriber);
    }

    @Override
    public Set<Property<?>> getMonitoredProperties() {
        // This is only to trigger the initial creation of the TagActor.
        // Once the TagActor is created, it will continue to receive changes, even if the
        //      actorFlagProperty isn't what changed.
        return Set.of(
                _actorFlagProperty
        );
    }

    public void initializeTagProvider(@NotNull TagProvider tagProvider) throws Exception {
        //noinspection ConstantValue
        if (tagProvider == null)
            throw new IllegalArgumentException("tagProvider cannot be null");

        if (tagProviderWasInitialized(tagProvider))
            throw new Exception(tagProvider.getName() + " tag provider was already initialized");

        // A TagActor must be created in order to handle any value changes.
        // To trigger the creation of an TagActor, a property (not value) must change.
        // Once a TagActor has been created, it will consume all value changes that
        //      occur for the tag that the given TagActor has been created for.
        // When an end user creates a new tag in the Designer via the Tag Editor window,
        //      the configuration of the given tag is saved, which updates properties,
        //      and causes a TagActor to be created.
        // When an end user modifies the value in the Designer Tag Browser 'Value' column,
        //      a configuration and property change are not executed. This does not
        //      cause a TagActor to be created.
        // When the gateway starts, existing tags in TagProviders might not have
        //      a property change to trigger the creation of a TagActor. So, when the user
        //      modifies value in the Designer Tag Browser 'Value' column, nothing will happen.
        // So, to force a property change on startup, use a dummy boolean property to write a
        //      property value to every tag in an existing provider, inducing a TagActor
        //      to be created for every tag in the TagProvider. However, if you set the
        //      property to true when it is already true, the property will not change,
        //      so a TagActor will not be created. So, write false then true to ensure
        //      that at least one change occurs and the tag actor is created.
        // However, when the TagProperties are changed below, at least one of them will
        //      cause a change which will cause processValue() to fire. So, you need to
        //      manually block processValue() from propagating downstream unless you
        //      intentionally want the current value to be fired as if it was a change event.

        List<TagPath> tagPaths = new ArrayList<>();
        for (var nodeDescription : browseRootRecursive(tagProvider))
            tagPaths.add(nodeDescription.getFullPath());

        // b1 = recursive (for UTDs, not children of folders.)
        // b2 = localPropsOnly
        var tagConfigurationModels = tagProvider.getTagConfigsAsync(tagPaths, false, true).get();

        List<TagConfiguration> tagConfigurations = new ArrayList<>();
        for (var tagConfigurationModel : tagConfigurationModels) {
            var tagConfiguration = tagConfigurationModel.getLocalConfiguration();
            tagConfiguration.set(_actorFlagProperty, true);
            tagConfigurations.add(tagConfigurationModel.getLocalConfiguration());
        }

        var results = tagProvider.saveTagConfigsAsync(tagConfigurations, CollisionPolicy.Abort).get();
        for (var result : results)
            if (result != QualityCode.Good)
                throw new Exception("Save true tag configuration returned not good quality code.");

        for (var tagConfiguration : tagConfigurations)
            tagConfiguration.set(_actorFlagProperty, false);

        results = tagProvider.saveTagConfigsAsync(tagConfigurations, CollisionPolicy.Abort).get();
        for (var result : results)
            if (result != QualityCode.Good)
                throw new Exception("Save false tag configuration returned not good quality code.");

        _initializedTagProviderNames.add(tagProvider.getName());
    }

    @Override
    public boolean isApplicable(PropertySet config) {
        // If you set this to false, it will shut down the actor
        return true;
    }

    public static void register(@NotNull TagConfigurationManager manager) {
        //noinspection ConstantValue
        if (manager == null)
            throw new IllegalArgumentException("manager can not be null");

        if (_isRegistered)
            throw new IllegalStateException("TagChangeActorFactory is already registered");

        manager.registerActorFactory(singleton);
        _isRegistered = true;
    }

    @Override
    public void shutdown() {
        _logger.info(TagChangeActorFactory.class.getSimpleName() + " Shutdown");
    }

    @Override
    public void startup(GatewayContext gatewayContext) {
        _logger.info(TagChangeActorFactory.class.getSimpleName() + " Startup");
    }

    public void subscribe(@NotNull ITagChangeSubscriber tagChangeSubscriber, @NotNull TagProvider tagProvider) {
        //noinspection ConstantValue
        if (tagChangeSubscriber == null)
            throw new IllegalArgumentException("tagChangeSubscriber can not be null");
        //noinspection ConstantValue
        if (tagProvider == null)
            throw new IllegalArgumentException("tagProvider can not be null");

        if (!_subscribers.containsKey(tagProvider.getName()))
            _subscribers.put(tagProvider.getName(), new ArrayList<>());
        var subscribers = _subscribers.get(tagProvider.getName());
        if (subscribers.contains(tagChangeSubscriber))
            throw new RuntimeException("ITagChangeSubscriber is already subscribed to " + tagProvider.getName());
        subscribers.add(tagChangeSubscriber);
    }

    public boolean tagProviderWasInitialized(@NotNull TagProvider tagProvider) {
        //noinspection ConstantValue
        if (tagProvider == null)
            throw new IllegalArgumentException("tagProvider cannot be null");

        return _initializedTagProviderNames.contains(tagProvider.getName());
    }

    public void unSubscribe(@NotNull ITagChangeSubscriber tagChangeSubscriber, @NotNull TagProvider tagProvider) {
        //noinspection ConstantValue
        if (tagChangeSubscriber == null)
            throw new IllegalArgumentException("tagChangeSubscriber can not be null");
        //noinspection ConstantValue
        if (tagProvider == null)
            throw new IllegalArgumentException("tagProvider can not be null");

        if (!_subscribers.containsKey(tagProvider.getName()))
            throw new RuntimeException("ITagChangeSubscriber is not subscribed to " + tagProvider.getName());
        var subscribers = _subscribers.get(tagProvider.getName());
        if (!subscribers.contains(tagChangeSubscriber))
            throw new RuntimeException("ITagChangeSubscriber is not subscribed to " + tagProvider.getName());
        subscribers.remove(tagChangeSubscriber);
    }

    public static void unregister(@NotNull TagConfigurationManager manager) {
        //noinspection ConstantValue
        if (manager == null)
            throw new IllegalArgumentException("manager can not be null");

        if (!_isRegistered)
            throw new IllegalStateException("TagChangeActorFactory is not registered");

        manager.unregisterActorFactory(singleton);
        _isRegistered = false;
    }

    private static final Property<Boolean> _actorFlagProperty =
            new BasicProperty<>("Hydra-MQTT-TagChangeActorFlag", Boolean.class);
    private static final int _actorPosition = 20001;
    public static final ActorClassification classification =
            new ActorClassification(TagChangeActorFactory.class.getName(), _actorPosition);
    private final List<String> _initializedTagProviderNames = new ArrayList<>();
    private static boolean _isRegistered = false;
    private final Logger _logger = GatewayHook.getLogger(getClass());
    private final HashMap<String, List<ITagChangeSubscriber>> _subscribers = new HashMap<>();

    private TagChangeActorFactory() {
    }

    private static List<NodeDescription> browseFolder(@NotNull TagProvider tagProvider, @NotNull TagPath tagPath) throws Exception {
        //noinspection DuplicatedCode,ConstantValue
        if (tagProvider == null)
            throw new RuntimeException("tagProvider can not be null");
        //noinspection ConstantValue
        if (tagPath == null)
            throw new RuntimeException("tagPath can not be null");

        var results = tagProvider.browseAsync(tagPath, BrowseFilter.NONE).get();

        if (results.getResultQuality().isNotGood())
            throw new Exception("Browse folder " + tagPath.toStringFull() + " node descriptions returned bad result.");

        var nodeDescriptions = results.getResults();
        if (nodeDescriptions == null)
            throw new Exception("Could not browse folder " + tagPath.toStringFull() + " node descriptions.");

        return nodeDescriptions.stream().toList();
    }

    private static List<NodeDescription> browseFolderRecursive(@NotNull TagProvider tagProvider, @NotNull TagPath tagPath) throws Exception {
        //noinspection DuplicatedCode,ConstantValue
        if (tagProvider == null)
            throw new RuntimeException("tagProvider can not be null");
        //noinspection ConstantValue
        if (tagPath == null)
            throw new RuntimeException("tagPath can not be null");
        List<NodeDescription> nodeDescriptions = new ArrayList<>();
        browseFolderRecursive(tagProvider, tagPath, nodeDescriptions);
        return nodeDescriptions;
    }

    private static void browseFolderRecursive(@NotNull TagProvider tagProvider, @NotNull TagPath tagPath, @NotNull List<NodeDescription> nodeDescriptions) throws Exception {
        //noinspection DuplicatedCode,ConstantValue
        if (tagProvider == null)
            throw new RuntimeException("tagProvider can not be null");
        //noinspection ConstantValue
        if (tagPath == null)
            throw new RuntimeException("tagPath can not be null");
        //noinspection ConstantValue
        if (nodeDescriptions == null)
            throw new RuntimeException("nodeDescriptions can not be null");

        for (var nodeDescription : browseFolder(tagProvider, tagPath)) {
            var childPath = nodeDescription.getFullPath();
            if (!nodeDescription.hasChildren())
                continue;
            // This occurs for UDTs
            if (childPath == null)
                continue;
            browseFolderRecursive(tagProvider, childPath, nodeDescriptions);
        }
    }

    private static List<NodeDescription> browseRootRecursive(@NotNull TagProvider tagProvider) throws Exception {
        //noinspection ConstantValue
        if (tagProvider == null)
            throw new RuntimeException("tagProvider can not be null");
        List<NodeDescription> nodeDescriptions = browseFolder(tagProvider, TagPathParser.parse(""));
        for (var nodeDescription : nodeDescriptions)
            if (nodeDescription.hasChildren())
                browseFolderRecursive(tagProvider, nodeDescription.getFullPath(), nodeDescriptions);
        return nodeDescriptions;
    }
}
