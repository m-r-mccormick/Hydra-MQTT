package com.mrmccormick.ignition.hydra.mqtt;

import java.util.*;

import com.mrmccormick.ignition.hydra.mqtt.data.DataCoder;
import com.mrmccormick.ignition.hydra.mqtt.data.DataEvent;
import com.mrmccormick.ignition.hydra.mqtt.data.DataEventSubscriber;
import org.apache.log4j.Logger;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttManager implements MqttCallback {

    private MqttClient _client;
    private String _brokerUrl;
    private final Logger _logger = GatewayHook.GetLogger(getClass());

    private final int _subQos;
    private final List<String> _subscriptions;
    private final DataCoder _dataCoder;

    public List<DataEventSubscriber> DataEventSubscribers = new ArrayList<>();

    public MqttManager(String host, int port, int subQos, DataCoder dataCoder, List<String> subscriptions) throws MqttException {
        if (host == null) {
            throw new IllegalArgumentException("host cannot be null");
        }
        if (port < 0) {
            throw new IllegalArgumentException("port cannot be negative");
        }
        _brokerUrl = "tcp://" + host + ":" + port;

        if (subQos < 0 || subQos > 2) {
            throw new IllegalArgumentException("subQos must be between 0 and 2");
        }
        _subQos = subQos;

        if (dataCoder == null) {
            throw new IllegalArgumentException("dataCoder can not be null");
        }
        _dataCoder = dataCoder;

        if (subscriptions == null) {
            _subscriptions = new ArrayList<>();
        } else {
            _subscriptions = subscriptions;
        }

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
        _logger.info("MQTT client started, connected to " + _brokerUrl + ".");
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
                _logger.error("Error creating a new connection to MQTT broker. Cause: " + me.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    public void Shutdown() throws Exception {
        _client.disconnect();
        _client.close();
        _logger.info("MQTT Client stopped.");
    }

    public boolean IsConnected() {
        return  _client.isConnected();
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
}
