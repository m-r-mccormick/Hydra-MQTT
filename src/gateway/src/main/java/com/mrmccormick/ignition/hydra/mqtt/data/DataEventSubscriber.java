package com.mrmccormick.ignition.hydra.mqtt.data;

public interface DataEventSubscriber {
    void HandleDataEvent(DataEvent dataEvent);
}
