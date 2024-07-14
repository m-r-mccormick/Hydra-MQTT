package com.mrmccormick.ignition.hydra.mqtt.data;

public interface DataCoder {
    DataEvent Decode(String path, byte[] bytes) throws Exception;
}
