package com.mrmccormick.ignition.hydra.mqtt.data;

public interface IDataCoder {
    byte[] Encode(DataEvent event) throws Exception;

    DataEvent Decode(String path, byte[] bytes) throws Exception;
}
