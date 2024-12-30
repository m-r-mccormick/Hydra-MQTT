package com.mrmccormick.ignition.hydra.mqtt;

import com.mrmccormick.ignition.hydra.mqtt.data.IDataCoder;
import com.mrmccormick.ignition.hydra.mqtt.data.DataEvent;
import com.mrmccormick.ignition.hydra.mqtt.data.IDataEventSubscriber;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import javax.net.ssl.*;
import javax.net.ssl.SSLSocketFactory;
import org.apache.log4j.Logger;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttManager implements MqttCallback, IDataEventSubscriber {

    private final Logger _logger = GatewayHook.GetLogger(getClass());

    private final MqttClient _client;
    private final String _brokerUrl;
    private final int _pubQos;
    private final int _subQos;
    private final String _writeSuffix;
    private final List<String> _subscriptions;
    private final IDataCoder _dataCoder;

    public List<IDataEventSubscriber> DataEventSubscribers = new ArrayList<>();

    public MqttManager(String host, int port, int pubQos, int subQos, IDataCoder dataCoder, String writeSuffix,
                       List<String> subscriptions) throws MqttException, CertificateException, IOException, InvalidKeySpecException, NoSuchAlgorithmException, KeyException {
        if (host == null) {
            throw new IllegalArgumentException("host cannot be null");
        }
        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }

        if (pubQos < 0 || pubQos > 2) {
            throw new IllegalArgumentException("pubQos must be between 0 and 2");
        }
        _pubQos = pubQos;

        if (subQos < 0 || subQos > 2) {
            throw new IllegalArgumentException("subQos must be between 0 and 2");
        }
        _subQos = subQos;

        if (dataCoder == null) {
            throw new IllegalArgumentException("dataCoder can not be null");
        }
        _dataCoder = dataCoder;

        _writeSuffix = writeSuffix;

        if (subscriptions == null) {
            _subscriptions = new ArrayList<>();
        } else {
            _subscriptions = subscriptions;
        }

        _brokerUrl = "tcp://" + host + ":" + port;
        String clientId = "Hydra-MQTT-" + UUID.randomUUID();
        _client = new MqttClient(_brokerUrl, clientId, new MemoryPersistence());
    }

    public void Startup(boolean cleanSession) throws Exception {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(cleanSession);

        if (_client.isConnected()) {
            _logger.info("Client is already connected.");
            return;
        }

        try {
            _client.connect(connOpts);
        } catch (MqttException e) {
            if (!Objects.equals(e.getMessage(), "Connect already in progress")) {
                throw e;
            }
            _logger.info("Connect already in progress...");
            return;
        } catch (Exception e) {
            throw e;
        }

        for (int i = 0; i < 20; i++) {
            if (!_client.isConnected()) {
                Thread.sleep(100);
                if (i % 10 == 0) {
                    _logger.debug("Waiting to connect...");
                }
            }
        }

        if (!_client.isConnected()) {
            _logger.warn("Connect timeout reached.");
        }

        _client.setCallback(this);
        for (String subscription : _subscriptions) {
            _client.subscribe(subscription, _subQos);
        }
    }

    public void Reconnect() {
        if (_client.isConnected()) {
            _logger.info("Already connected to MQTT broker, can not reconnect.");
            return;
        }

        _logger.info("Attempting to reconnect to MQTT broker...");

        try {
            _client.reconnect();
            _logger.info("Reconnected to MQTT broker.");
        } catch (Exception e) {
            _logger.error("Could not reconnect to MQTT broker. Cause: " + e.getMessage(), e);

            try {
                Thread.sleep(1000);
            } catch (Exception se) {
                _logger.error("Could not sleep.");
                return;
            }

            _logger.info("Attempting to create new connection to MQTT broker...");
            try {
                Startup(true);
            } catch (MqttException me) {
                _logger.error("Could not create a new connection to MQTT broker. Cause: " + me.getMessage(), e);
            } catch (Exception me) {
                throw new RuntimeException(e);
            }
        }
    }

    public void Shutdown() throws Exception {
        _client.disconnect();
        _client.close();
    }

    public boolean IsConnected() {
        return  _client.isConnected();
    }

    public void Publish(DataEvent dataEvent) throws Exception {
        MqttMessage message = new MqttMessage(_dataCoder.Encode(dataEvent));
        message.setQos(_pubQos);

        String basePath;
        String path;
        if (dataEvent.PathOverride == null) {
            // Append the _writeSuffix if available
            basePath = dataEvent.Path;
            if (_writeSuffix == null) {
                path = basePath;
            } else {
                path = basePath + _writeSuffix;
            }
        } else {
            // Do not append _writeSuffix when overriding the path
            basePath = dataEvent.PathOverride;
            path = basePath;
        }

        _client.publish(path, message);
    }

    @Override
    public void connectionLost(Throwable cause) {
        _logger.info("MQTT client connection to " + _brokerUrl + " lost. Cause: " + cause.getMessage());
        try {
            _client.reconnect();
        } catch (MqttException e) {
            _logger.error("Connection to MQTT broker lost. Could not re-establish connection. Cause: " + cause.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (_writeSuffix != null) {
            if (topic.endsWith(_writeSuffix)) {
                return;
            }
        }

        DataEvent event;
        try {
            event = _dataCoder.Decode(topic, message.getPayload());
        } catch (Exception e) {
            return;
        }

        for (var handler : DataEventSubscribers) {
            try {
                handler.HandleDataEvent(event);
            } catch (Exception e) {
                _logger.error("Could not handle data event: " + handler.getClass(), e);
            }
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    @Override
    public void HandleDataEvent(DataEvent dataEvent) {
        try {
            Publish(dataEvent);
        } catch (Exception e) {
            _logger.error("Failed to publish to MQTT broker " + e.getMessage());
        }
    }
}
