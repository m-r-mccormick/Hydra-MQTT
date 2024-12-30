package com.mrmccormick.ignition.hydra.mqtt.settings;

import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import com.mrmccormick.ignition.hydra.mqtt.Connection;

public interface IConnectSettings {
    public boolean getEnabled();
    public Connection getConnection(GatewayContext context) throws Exception;
}
