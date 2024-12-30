package com.mrmccormick.ignition.hydra.mqtt.data;

public interface IDataEventSubscriber {
    void HandleDataEvent(DataEvent dataEvent);
}
