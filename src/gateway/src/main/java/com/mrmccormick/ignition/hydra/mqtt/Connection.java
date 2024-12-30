package com.mrmccormick.ignition.hydra.mqtt;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import org.apache.log4j.Logger;

public class Connection {

    public final String name;

    private final Logger _logger = GatewayHook.GetLogger(getClass());

    private final TagManager _tagManagerPublish;
    private final TagManager _tagManagerSubscribe;
    private final MqttManager _mqttManager;

    public Connection(String name, MqttManager mqttManager, TagManager tagManagerPublish, TagManager tagManagerSubscribe) {
        if (name == null)
            throw new IllegalArgumentException("name can not be null");
        this.name = name;

        if (mqttManager == null)
            throw new IllegalArgumentException("mqttManager can not be null");
        _mqttManager = mqttManager;

        _tagManagerPublish = tagManagerPublish;
        _tagManagerSubscribe = tagManagerSubscribe;


        try {
            if (_tagManagerPublish != null) {
                _tagManagerPublish.DataEventSubscribers.add(_mqttManager);
            }
            if (_tagManagerSubscribe != null) {
                _mqttManager.DataEventSubscribers.add(_tagManagerSubscribe);
            }
        } catch (Exception e) {
            _logger.error("Error setting up " + name + ": " + e.getMessage(), e);
            throw e;
        }
    }

    public void Start(GatewayContext _context) throws Exception {
        if (_tagManagerPublish != null)
            _tagManagerPublish.Startup();
        if (_tagManagerSubscribe != null)
            _tagManagerSubscribe.Startup();

        _mqttManager.Startup(true);
    }

    public void Stop() throws Exception {
        if (_mqttManager != null) {
            _mqttManager.Shutdown();
        }

        if (_tagManagerPublish != null) {
            _tagManagerPublish.Shutdown();
        }
        if (_tagManagerSubscribe != null) {
            _tagManagerSubscribe.Shutdown();
        }
    }

    public void maintain(){
        if (!_mqttManager.IsConnected())
            _mqttManager.Reconnect();
    }
}
