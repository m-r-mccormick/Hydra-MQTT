package com.mrmccormick.hydra.mqtt.domain.actor.connector;

import com.mrmccormick.hydra.mqtt.domain.actor.IActor;

public interface IConnector extends IActor {
    void abortConnecting();

    void connect() throws Exception;

    void disconnect();

    boolean isConnected();

    boolean isConnecting();

    boolean isDisconnecting();

    boolean isEnabled();

    void setEnabled(boolean enabled);
}
