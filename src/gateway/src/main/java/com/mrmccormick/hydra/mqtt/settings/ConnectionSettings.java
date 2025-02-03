package com.mrmccormick.hydra.mqtt.settings;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.mrmccormick.hydra.mqtt.domain.actor.IActor;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.IConnector;
import com.mrmccormick.hydra.mqtt.domain.settings.TimestampFormat;
import com.mrmccormick.hydra.mqtt.domain.settings.TimestampIntegerFormat;
import com.mrmccormick.hydra.mqtt.implementation.actor.BufferActor;
import com.mrmccormick.hydra.mqtt.implementation.actor.RunnerBuilder;
import com.mrmccormick.hydra.mqtt.implementation.actor.connector.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ConnectionSettings {
    public final @Nullable String brokerHost;
    public final @Nullable Integer brokerPort;
    public final @Nullable Integer brokerPublishQos;
    public final @Nullable Integer brokerSubscribeQos;
    public final @Nullable String brokerSubscriptions;
    public final @Nullable Boolean connectionEnabled;
    public final @Nullable Boolean experimentalGlobalSubscription;
    public final @Nullable String representationPublishDocumentationPath;
    public final @Nullable TimestampFormat representationPublishTimestampFormat;
    public final @Nullable String representationPublishTimestampPath;
    public final @Nullable String representationPublishUnitsPath;
    public final @Nullable String representationPublishValuePath;
    public final @Nullable String representationSubscribeDocumentationPaths;
    public final @Nullable TimestampIntegerFormat representationSubscribeTimestampIntegerFormat;
    public final @Nullable String representationSubscribeTimestampPaths;
    public final @Nullable String representationSubscribeUnitsPaths;
    public final @Nullable String representationSubscribeValuePaths;
    public final @Nullable Boolean routingPublishPropertiesEnabled;
    public final @Nullable String routingPublishTopicSuffix;
    public final @Nullable String tagProviderPub;
    public final @Nullable String tagProviderSub;

    public ConnectionSettings(
            @NotNull GatewayContext gatewayContext,
            @Nullable Boolean connectionEnabled,
            @Nullable String brokerHost,
            @Nullable Integer brokerPort,
            @Nullable Integer brokerPublishQos,
            @Nullable Integer brokerSubscribeQos,
            @Nullable String brokerSubscriptions,
            @Nullable String tagProviderPub,
            @Nullable String tagProviderSub,
            @Nullable String routingPublishTopicSuffix,
            @Nullable Boolean routingPublishPropertiesEnabled,
            @Nullable String representationPublishValuePath,
            @Nullable String representationPublishTimestampPath,
            @Nullable TimestampFormat representationPublishTimestampFormat,
            @Nullable String representationPublishDocumentationPath,
            @Nullable String representationPublishUnitsPath,
            @Nullable String representationSubscribeValuePaths,
            @Nullable String representationSubscribeTimestampPaths,
            @Nullable TimestampIntegerFormat representationSubscribeTimestampIntegerFormat,
            @Nullable String representationSubscribeDocumentationPaths,
            @Nullable String representationSubscribeUnitsPaths,
            @Nullable Boolean experimentalGlobalSubscription
    ) {
        //noinspection ConstantValue
        if (gatewayContext == null)
            throw new IllegalArgumentException("gatewayContext can not be null");
        _gatewayContext = gatewayContext;

        this.connectionEnabled = connectionEnabled;

        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.brokerPublishQos = brokerPublishQos;
        this.brokerSubscribeQos = brokerSubscribeQos;
        this.brokerSubscriptions = brokerSubscriptions;

        this.tagProviderPub = tagProviderPub;
        this.tagProviderSub = tagProviderSub;

        this.routingPublishTopicSuffix = routingPublishTopicSuffix;
        this.routingPublishPropertiesEnabled = routingPublishPropertiesEnabled;

        this.representationPublishValuePath = representationPublishValuePath;
        this.representationPublishTimestampPath = representationPublishTimestampPath;
        this.representationPublishTimestampFormat = representationPublishTimestampFormat;
        this.representationPublishDocumentationPath = representationPublishDocumentationPath;
        this.representationPublishUnitsPath = representationPublishUnitsPath;
        this.representationSubscribeValuePaths = representationSubscribeValuePaths;
        this.representationSubscribeTimestampPaths = representationSubscribeTimestampPaths;
        this.representationSubscribeTimestampIntegerFormat = representationSubscribeTimestampIntegerFormat;
        this.representationSubscribeDocumentationPaths = representationSubscribeDocumentationPaths;
        this.representationSubscribeUnitsPaths = representationSubscribeUnitsPaths;

        this.experimentalGlobalSubscription = experimentalGlobalSubscription;
    }

    public @Nullable IConnector getExternalConnector() {
        return _externalConnector;
    }

    public static List<String> lineToPayloadPathComponents(@Nullable String lineString) {
        if (lineString == null)
            return Collections.emptyList();

        //noinspection ConstantValue
        if (!(lineString instanceof String))
            return Collections.emptyList();

        var lineComponents = lineString.trim().split("\\.");
        //noinspection ConstantValue
        if (lineComponents == null)
            return Collections.emptyList();

        List<String> components = new ArrayList<>();
        for (var lineComponent : lineComponents) {
            if (lineComponent == null || lineComponent.isEmpty())
                continue;
            components.add(lineComponent.trim());
        }

        return components;
    }

    public List<IActor> load(String connectionName) throws Exception {
        if (connectionName == null || connectionName.isEmpty())
            throw new IllegalArgumentException("connectionName cannot be empty");

        if (brokerHost == null || brokerHost.isEmpty())
            throw new IllegalArgumentException("brokerHost can not be empty");
        if (brokerPort == null)
            throw new IllegalArgumentException("brokerPort can not be empty");
        if (brokerPublishQos == null)
            throw new IllegalArgumentException("brokerPublishQos can not be empty");
        if (brokerSubscribeQos == null)
            throw new IllegalArgumentException("brokerSubscribeQos can not be empty");

        if (routingPublishPropertiesEnabled == null)
            throw new IllegalArgumentException("routingPublishPropertiesEnabled can not be empty");

        if (representationPublishValuePath == null || representationPublishValuePath.isEmpty())
            throw new IllegalArgumentException("representationPublishValuePath can not be empty");
        if (representationPublishTimestampPath == null)
            throw new IllegalArgumentException("representationPublishTimestampPath can not be empty");
        if (representationPublishTimestampFormat == null)
            throw new IllegalArgumentException("representationPublishTimestampFormat can not be empty");
        // RepresentationPublishDocumentationPath can be empty
        // RepresentationPublishUnitsPath can be empty
        if (representationSubscribeValuePaths == null)
            throw new IllegalArgumentException("representationSubscribeValuePaths can not be empty");
        if (representationSubscribeTimestampIntegerFormat == null)
            throw new IllegalArgumentException("representationSubscribeTimestampIntegerFormat can not be empty");
        // RepresentationSubscribeDocumentationPaths can be empty
        // RepresentationSubscribeUnitsPaths can be empty

        IConnector tagManagerPublish = null;
        IConnector tagManagerSubscribe = null;
        if (experimentalGlobalSubscription != null && experimentalGlobalSubscription) {
            if (tagProviderPub != null)
                tagManagerPublish = new TagEngineConnector(
                        connectionName,
                        "Publish-Only Tag Provider",
                        _gatewayContext,
                        tagProviderPub,
                        routingPublishPropertiesEnabled,
                        new RunnerBuilder(_gatewayContext)
                );

            if (tagProviderSub != null)
                tagManagerSubscribe = new TagEngineConnector(
                        connectionName,
                        "Subscribe-Only Tag Provider",
                        _gatewayContext,
                        tagProviderSub,
                        routingPublishPropertiesEnabled,
                        new RunnerBuilder(_gatewayContext)
                );
        } else {
            if (tagProviderPub != null)
                tagManagerPublish = new TagProviderConnector(
                        connectionName,
                        "Publish-Only Tag Provider",
                        _gatewayContext,
                        tagProviderPub,
                        routingPublishPropertiesEnabled);

            if (tagProviderSub != null)
                tagManagerSubscribe = new TagProviderConnector(
                        connectionName,
                        "Subscribe-Only Tag Provider",
                        _gatewayContext,
                        tagProviderSub,
                        routingPublishPropertiesEnabled);
        }

        JsonCoder coder = new JsonCoder(
                multilineToPayloadPaths(representationSubscribeValuePaths),
                multilineToPayloadPaths(representationSubscribeTimestampPaths),
                multilineToPayloadPaths(representationSubscribeDocumentationPaths),
                multilineToPayloadPaths(representationSubscribeUnitsPaths),
                representationSubscribeTimestampIntegerFormat,
                lineToPayloadPathComponents(representationPublishValuePath),
                lineToPayloadPathComponents(representationPublishTimestampPath),
                lineToPayloadPathComponents(representationPublishDocumentationPath),
                lineToPayloadPathComponents(representationPublishUnitsPath),
                representationPublishTimestampFormat
        );

        var externalConnector = new PahoMqtt3Connector(
                connectionName,
                brokerHost,
                brokerPort,
                brokerPublishQos,
                brokerSubscribeQos,
                routingPublishTopicSuffix,
                multilineToStringList(brokerSubscriptions),
                coder);

        List<IConnector> connectors = new ArrayList<>();
        connectors.add(externalConnector);

        // -------------------------------------------------------------------
        // Assemble Subscribe Actor Pipeline
        // Going from MQTT client to Sub Tag Provider

        List<IActor> subscribeActors = new ArrayList<>();

        var subBuffer = new BufferActor(connectionName, "Subscribe Buffer", new RunnerBuilder(_gatewayContext));
        subscribeActors.add(subBuffer);

        // Connect Inbound Subscribe Actors
        if (tagManagerSubscribe != null) {
            connectors.add(tagManagerSubscribe);
            IActor last = externalConnector;
            for (IActor current : subscribeActors) {
                last.addSubscriber(current);
                last = current;
            }
            last.addSubscriber(tagManagerSubscribe);
        }

        // -------------------------------------------------------------------
        // Assemble Publish Actor Pipeline
        // Going from Pub Tag Provider to MQTT Client

        List<IActor> publishActors = new ArrayList<>();

        // Connect Outbound Publish Actors
        if (tagManagerPublish != null) {
            connectors.add(tagManagerPublish);
            IActor last = tagManagerPublish;
            for (IActor current : publishActors) {
                last.addSubscriber(current);
                last = current;
            }
            last.addSubscriber(externalConnector);
        }

        // -------------------------------------------------------------------
        // Assemble Master Actor List

        // Needs to be in order from MQTT client to Tag Engine and back to MQTT client
        //  to ensure that when they are cleared/disconnected, they are done so in the
        //  direction of data flow. This ensures that upstream data flow is stopped
        //  before attempting to clear/disconnect a given actor. Otherwise, data will
        //  continue to flow into a given actor while trying to clear/disconnect it.
        List<IActor> allActors = new ArrayList<>();
        allActors.addAll(connectors);
        allActors.addAll(subscribeActors);
        allActors.addAll(publishActors);

        _externalConnector = externalConnector;
        return allActors;
    }

    public static List<List<String>> multilineToPayloadPaths(@Nullable String multilineString) {
        if (multilineString == null)
            return Collections.emptyList();

        //noinspection ConstantValue
        if (!(multilineString instanceof String))
            return Collections.emptyList();

        List<List<String>> paths = new ArrayList<>();
        for (var line : multilineToStringList(multilineString)) {
            List<String> components = lineToPayloadPathComponents(line);
            if (!components.isEmpty())
                paths.add(components);
        }

        return paths;
    }

    public static List<String> multilineToStringList(@Nullable String multilineString) {
        if (multilineString == null)
            return Collections.emptyList();

        //noinspection ConstantValue
        if (!(multilineString instanceof String))
            return Collections.emptyList();

        List<String> values = new LinkedList<>();
        for (var line : multilineString.replace("\r", "").split("\n")) {
            if (line == null || line.isEmpty())
                continue;
            values.add(line.trim());
        }

        return values;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();

        if (connectionEnabled == null)
            errors.add("The value of Connection Enabled can not be empty.");

        if (brokerHost == null || brokerHost.isEmpty())
            errors.add("The value of Broker Host can not be empty.");
        if (brokerPort == null)
            errors.add("The value of Broker Port can not be empty.");
        errors.addAll(validateQos("Broker Publish QoS.", brokerPublishQos));
        errors.addAll(validateQos("Broker Subscribe QoS.", brokerSubscribeQos));
        // Broker Subscriptions can be null

        // Tag Provider Pub can be null
        // Tag Provider Sub can be null


        if ((tagProviderPub == null || tagProviderPub.isEmpty()) &&
                (tagProviderSub == null || tagProviderSub.isEmpty())
        )
            errors.add("A Publish-Only Tag Provider or Subscribe-Only Tag Provider must be specified.");
        if ((tagProviderPub != null && !tagProviderPub.isEmpty()) && Objects.equals(tagProviderPub, tagProviderSub))
            errors.add("Publish-Only Tag Provider and Subscribe-Only Tag Provider can not be the same.");
        var tagManager = _gatewayContext.getTagManager();
        if ((tagProviderSub != null && !tagProviderSub.isEmpty()) && tagManager.getTagProvider(tagProviderSub) == null)
            errors.add("The specified Subscribe-Only Tag Provider " + tagProviderSub + " does not exist.");
        if ((tagProviderPub != null && !tagProviderPub.isEmpty()) && tagManager.getTagProvider(tagProviderPub) == null)
            errors.add("The specified Publish-Only Tag Provider " + tagProviderPub + " does not exist.");


        // Routing Publish Topic Suffix can be null
        if (routingPublishPropertiesEnabled == null)
            errors.add("The value of Publish Routing Properties Enabled can not be empty.");


        if (representationPublishValuePath == null || representationPublishValuePath.isEmpty())
            errors.addAll(validateLineToPayloadPathComponents("Representation Publish Value Path", representationPublishValuePath));
        // Representation Publish Timestamp Path can be empty
        if (representationPublishTimestampPath == null || representationPublishTimestampPath.isEmpty())
            errors.addAll(validateLineToPayloadPathComponents("Representation Publish Timestamp Path", representationPublishTimestampPath));
        if (representationPublishTimestampFormat == null)
            errors.add("The value of Representation Publish Timestamp Format can not be empty");
        // RepresentationPublishDocumentationPath can be empty
        // RepresentationPublishUnitsPath can be empty
        if (representationSubscribeValuePaths == null || representationSubscribeValuePaths.isEmpty())
            errors.addAll(validatePayloadPaths("Representation Subscribe Value Paths", representationSubscribeValuePaths));
        // Representation Subscribe Timestamp Paths can be empty
        if (representationSubscribeTimestampIntegerFormat == null)
            errors.add("The value of Representation Publish Timestamp Integer Format can not be empty.");
        // RepresentationSubscribeDocumentationPaths can be empty
        // RepresentationSubscribeUnitsPaths can be empty

        return errors;
    }

    public static List<String> validateLineToPayloadPathComponents(@NotNull String name, @Nullable Object value) {
        //noinspection DuplicatedCode,ConstantValue
        if (name == null)
            throw new NullPointerException("name can not be null");

        if (value == null)
            return new ArrayList<>(List.of("The value of '" + name + "' can not be null."));

        if (!(value instanceof String))
            return new ArrayList<>(List.of("The value of '" + name + "' must be a string."));

        List<String> errors = new ArrayList<>();

        List<String> components = new ArrayList<>();
        for (var lineComponent : lineToPayloadPathComponents((String) value)) {
            if (lineComponent == null || lineComponent.isEmpty())
                continue;
            components.add(lineComponent.trim());
        }

        if (components.isEmpty())
            errors.add("The value of '" + name + "' can not be empty");

        return errors;
    }

    public static List<String> validatePayloadPaths(@NotNull String name, @Nullable Object value) {
        //noinspection DuplicatedCode,ConstantValue
        if (name == null)
            throw new NullPointerException("name can not be null");

        if (value == null)
            return new ArrayList<>(List.of("The value of '" + name + "' can not be null."));

        if (!(value instanceof String))
            return new ArrayList<>(List.of("The value of '" + name + "' must be a string."));

        List<String> errors = new ArrayList<>();
        var paths = multilineToPayloadPaths((String) value);
        for (var path : paths) {
            if (path.isEmpty())
                errors.add("The value of lines in '" + name + "' can not be empty.");
        }

        if (!errors.isEmpty())
            errors.add("The value of '" + name + "' can not be empty.");

        return errors;
    }

    public static List<String> validateQos(@NotNull String name, @Nullable Object value) {
        //noinspection ConstantValue,DuplicatedCode
        if (name == null)
            throw new NullPointerException("name can not be null");

        if (value == null)
            return new ArrayList<>(List.of("The value of '" + name + "' can not be null."));

        if (!(value instanceof Integer))
            return new ArrayList<>(List.of("The value of '" + name + "' must be an integer."));

        if ((Integer) value < 0 || (Integer) value > 2)
            return new ArrayList<>(List.of("The value of '" + name + "' must be between 0 and 2."));

        return List.of();
    }

    private @Nullable IConnector _externalConnector;
    private final @NotNull GatewayContext _gatewayContext;
}
