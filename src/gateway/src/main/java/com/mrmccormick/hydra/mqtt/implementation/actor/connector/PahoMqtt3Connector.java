package com.mrmccormick.hydra.mqtt.implementation.actor.connector;

import com.mrmccormick.hydra.mqtt.GatewayHook;
import com.mrmccormick.hydra.mqtt.domain.Event;
import com.mrmccormick.hydra.mqtt.domain.actor.IActor;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.ICoder;
import com.mrmccormick.hydra.mqtt.domain.actor.connector.IConnector;

import java.util.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PahoMqtt3Connector implements MqttCallback, IConnector {

    public PahoMqtt3Connector(@NotNull String connectionName, @NotNull String host, int port, int pubQos, int subQos,
                              @Nullable String writeSuffix, @Nullable List<String> subscriptions,
                              @Nullable ICoder coder) {
        //noinspection ConstantValue
        if (connectionName == null)
            throw new IllegalArgumentException("connectionName cannot be null");
        _logger = GatewayHook.getLogger(connectionName + "." + getClass().getSimpleName());

        //noinspection ConstantValue
        if (host == null)
            throw new IllegalArgumentException("host cannot be null");
        _host = host;

        if (port < 0)
            throw new IllegalArgumentException("port cannot be negative");
        _port = port;

        if (pubQos < 0 || pubQos > 2)
            throw new IllegalArgumentException("pubQos must be between 0 and 2");
        _pubQos = pubQos;

        if (subQos < 0 || subQos > 2)
            throw new IllegalArgumentException("subQos must be between 0 and 2");
        _subQos = subQos;

        _writeSuffix = writeSuffix;

        _subscriptions = Objects.requireNonNullElseGet(subscriptions, ArrayList::new);

        if (coder == null)
            throw new IllegalArgumentException("coder can not be null");
        _coder = coder;
    }

    @Override
    public void abortConnecting() {
        _abortConnecting = true;
    }

    public void addSubscriber(@NotNull IActor subscriber) {
        if (_subscribers.contains(subscriber)) {
            _logger.error("Subscriber is already subscribed");
            return;
        }
        _subscribers.add(subscriber);
    }

    public boolean clear() {
        return true;
    }

    public void connect() {
        if (_isConnecting) {
            _logger.debug("Connect requested but already connecting. Ignoring connect request.");
            return;
        }

        if (_client != null && _client.isConnected()) {
            _logger.debug("Connect requested but already connected. Ignoring connect request.");
            return;
        }

        _isConnecting = true;

        // Check whether the client has already been initialized
        if (_client == null || !_connectInitialized) {
            // The client has not been initialized, so initialize it
            connectByType(2, 1);
            if (!_client.isConnected()) {
                _logger.error("Could not connect.");
            }
            _isConnecting = false;
            _abortConnecting = false;
            return;
        }

        // The client has been initialized, so attempt to reconnect
        if (_client.isConnected()) {
            _logger.warn("Already connected to a MQTT broker, will not attempt to reconnect.");
            _isConnecting = false;
            _abortConnecting = false;
            return;
        }

        int connectAttempts = 5;
        connectByType(0, connectAttempts);

        if (_client.isConnected() || _abortConnecting) {
            _isConnecting = false;
            _abortConnecting = false;
            return;
        }

        connectByType(1, connectAttempts);

        if (_client.isConnected() || _abortConnecting) {
            _isConnecting = false;
            _abortConnecting = false;
            return;
        }

        connectByType(2, connectAttempts);

        if (_client.isConnected() || _abortConnecting) {
            _isConnecting = false;
            _abortConnecting = false;
            return;
        }

        _logger.error("Reconnect attempts failed.");
        _isConnecting = false;
        _abortConnecting = false;
    }

    @Override
    public void connectionLost(@Nullable Throwable cause) {

        String message = "";
        if (cause != null) {
            message = cause.getMessage();
            if (cause.getCause() != null)
                if (cause.getCause().getMessage() != null)
                    message += ", " + cause.getCause().getMessage();
        }

        _logger.warn("Lost connection detected: " + message);
        try {
            connect();
            _logger.debug("Lost connection reestablished");
        } catch (Exception e) {
            _logger.error("Could not reconnect: " + message +
                    ", Exception: " + e.getMessage(), e);
        }
    }

    @Override
    public void deliveryComplete(@Nullable IMqttDeliveryToken token) {

    }

    public void disconnect() {
        if (_client == null) {
            _logger.error("Can not disconnect because a connection has not been initialized.");
            return;
        }
        _isDisconnecting = true;
        try {
            _client.disconnect();
            _client.close();
        } catch (Exception e) {
            _logger.error("Failed to disconnect from MQTT broker: " + e.getMessage(), e);
        }
        _isDisconnecting = false;
    }

    public @NotNull String getName() {
        return "Paho MQTT v3 Connector";
    }

    @Override
    public @NotNull List<IActor> getSubscribers() {
        return new ArrayList<>(_subscribers);
    }

    public boolean isConnected() {
        if (_client == null) {
            return false;
        }
        return _client.isConnected();
    }

    public boolean isConnecting() {
        return _isConnecting;
    }

    public boolean isDisconnecting() {
        return _isDisconnecting;
    }

    public boolean isEnabled() {
        return _enabled;
    }

    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    public boolean isSubscriber(@NotNull IActor subscriber) {
        //noinspection ConstantValue
        if (subscriber == null)
            throw new IllegalArgumentException("subscriber cannot be null");
        return _subscribers.contains(subscriber);
    }

    @Override
    public void messageArrived(@Nullable String topic, @Nullable MqttMessage message) {
        // Entire function wrapped in try to avoid killing any background threads calling this method
        try {
            if (topic == null) {
                _logger.warn("Received MQTT message with null topic. Discarding message.");
                return;
            }

            if (message == null) {
                _logger.warn("Received MQTT message with null message. Discarding message.");
                return;
            }

            if (!_enabled) {
                return;
            }

            if (_writeSuffix != null) {
                if (topic.endsWith(_writeSuffix)) {
                    return;
                }
            }

            Event event;
            try {
                event = _coder.decode(topic, message.getPayload());
            } catch (Exception e) {
                return;
            }

            for (var subscriber : _subscribers) {
                try {
                    subscriber.receive(event);
                } catch (Exception e) {
                    _logger.error("Could not handle data event: " + subscriber.getClass(), e);

                    _logger.error("messageArrived() DataEventSubscriber " + subscriber.getClass().getName() +
                            " -> " + subscriber.getClass().getSimpleName() + " failed to handle DataEvent: " +
                            e.getMessage(), e);
                    return;
                }
            }
        } catch (Exception e) {
            _logger.error("messageArrived() unexpected error: " + e.getMessage(), e);
        }
    }

    public void receive(@NotNull Event event) {
        //noinspection ConstantValue
        if (event == null)
            throw new NullPointerException("event can not be null");

        if (!_enabled) {
            return;
        }

        byte[] payload;
        try {
            payload = _coder.encode(event);
        } catch (Exception e) {
            _logger.error(getName() + " could not get encode model for " +
                    event.path + ": " + e.getMessage(), e);
            return;
        }

        String topic;
        // Append the _writeSuffix if available
        var basePath = event.path;
        if (_writeSuffix == null) {
            topic = basePath;
        } else {
            topic = basePath + _writeSuffix;
        }

        MqttMessage message = new MqttMessage(payload);
        message.setQos(_pubQos);

        if (_client == null)
            throw new RuntimeException("Client has not been initialized.");

        try {
            _client.publish(topic, message);
        } catch (Exception e) {
            _logger.error(getName() + " could not publish payload for " +
                    event.path + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void removeAllSubscribers() {
        _subscribers.clear();
    }

    public void removeSubscriber(@NotNull IActor subscriber) {
        if (!_subscribers.contains(subscriber)) {
            _logger.error("Subscriber is not subscribed");
            return;
        }
        _subscribers.remove(subscriber);
    }

    private boolean _abortConnecting = false;
    private @Nullable MqttClient _client = null;
    private final @NotNull ICoder _coder;
    private boolean _connectInitialized = false;
    private boolean _enabled = true;
    private final @NotNull String _host;
    private boolean _isConnecting = false;
    private boolean _isDisconnecting = false;
    private final @NotNull Logger _logger;
    private final int _port;
    private final int _pubQos;
    private final int _subQos;
    private final @NotNull List<IActor> _subscribers = new ArrayList<>();
    private final @NotNull List<String> _subscriptions;
    private final @Nullable String _writeSuffix;

    private synchronized void connectByType(int type, int connectAttempts) {
        var rt = "Hard"; // Reconnect Type
        if (type <= 0)
            rt = "Soft"; // Reconnect Type
        if (type >= 2)
            rt = "Initialize"; // Reconnect Type

        var sv = "resubscribe"; // Subscribe Verb
        var cv = "reconnect"; // Connect Verb
        if (rt.equals("Initialize"))
            cv = "reconnection"; // Connect Verb

        if (_client == null || !_connectInitialized) {
            sv = "subscribe"; // Subscribe Verb
            cv = "connect"; // Connect Verb
            if (rt.equals("Initialize"))
                cv = "connection"; // Connect Verb

            if (rt.equals("Soft")) {
                _logger.warn("Has not been initialized, will not attempt " + rt + " " + cv + ".");
                return;
            }
        } else {
            if (_client.isConnected()) {
                _logger.warn("Already connected to a MQTT broker, will not attempt " + rt + " " + cv + ".");
                return;
            }
        }

        for (int i = 0; i < connectAttempts; i++) {
            Exception ex = null;
            try {
                switch (rt) {
                    case "Soft" -> _client.reconnect();
                    case "Hard" -> initialize(false, false);
                    case "Initialize" -> initialize(true, true);
                    default -> {
                        _logger.error("Invalid " + cv + " type: " + rt);
                        return;
                    }
                }
            } catch (Exception e) {
                ex = e;
            }

            int connectSleepMs = 10000;
            if (i == 0 && rt.equals("Initialize"))
                connectSleepMs = 1000;
            int sleepCount = 100;
            try {
                for (int j = 0; j < sleepCount; j++) {
                    Thread.sleep(connectSleepMs / sleepCount);

                    if (_client.isConnected())
                        break;

                    if (_abortConnecting) {
                        _logger.info("Abort connecting request received.");
                        return;
                    }
                }
            } catch (Exception e) {
                _logger.error("Could not sleep between " + rt + " " + cv + " attempts: " + e, e);
            }

            if (!_client.isConnected()) {
                if (ex == null)
                    _logger.warn(rt + " " + cv + " attempt " + (i + 1) + " of " + connectAttempts +
                            " failed");
                else
                    _logger.warn(rt + " " + cv + " attempt " + (i + 1) + " of " + connectAttempts +
                            " failed: " + ex, ex);
                continue;
            }

            // On a soft reconnect, need to resubscribe to all subscriptions
            if (rt.equals("Soft")) {
                // Do not need to call _client.setCallback(this), client remembers previously set callbacks.

                for (String subscription : _subscriptions) {
                    try {
                        _client.subscribe(subscription, _subQos);
                    } catch (Exception e) {
                        _logger.warn(rt + " " + cv + " could not " + sv + " to " + subscription +
                                " after " + cv + "ing to MQTT broker: " + e, e);
                    }
                }
            }

            _logger.info(rt + " " + cv + " attempt was successful.");
            break;
        }
    }

    private void initialize(boolean cleanSession, boolean newClient) throws Exception {
        String brokerUrl = "ssl://" + _host + ":" + _port;

        if (newClient || _client == null) {
            String clientId = "Hydra-MQTT-" + UUID.randomUUID();
            _client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        }

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(cleanSession);

        try {
            _client.connect(connOpts);
        } catch (MqttException e) {
            if (!Objects.equals(e.getMessage(), "Connect already in progress")) {
                throw e;
            }
            _logger.info("Connect attempt already in progress, aborting connect attempt.");
            return;
        }

        int timeoutMs = 2000;
        int sleepMs = 100;
        for (int i = 0; i < (timeoutMs / sleepMs); i++) {
            if (_client.isConnected())
                break;
            Thread.sleep(sleepMs);
        }

        if (!_client.isConnected())
            throw new Exception("Connect timeout threshold of " + timeoutMs + " ms exceeded.");

        _client.setCallback(this);
        for (String subscription : _subscriptions) {
            _client.subscribe(subscription, _subQos);
        }

        _connectInitialized = true;
    }
}
