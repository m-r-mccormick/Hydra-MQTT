---
title: 'Changelog'
---

# v0.x


## v0.1.0

- __Built-In Documentation__: Documentation is now built into the module and can be accessed through the gateway.
    - Open built-in documentation by navigating to `Config > System > Modules > Hydra-MQTT > More > Documentation`.

- __Automatically Load Saved Configuration Changes__: When configuration changes are saved, the new configuration is
  automatically loaded.
    - Saving a configuration will immediately disconnect from, then reconnect to, the broker.
    - This only occurs if the saved configuration is valid. Configuration errors will appear in the log.
        - If the saved configuration is invalid, the previous configuration will continue to be used until a valid
          configuration is saved.
        - If the gateway or the Hydra-MQTT module is restarted and an invalid configuration was the last configuration 
          to be saved, after saving a valid configuration, manually restarting the module may be required.
    - In `v0.0.2`, a manual Hydra-MQTT module restart was required for saved configuration changes to take effect.

- __Poll For Initial Connection__: When the Hydra-MQTT module is started or configuration changes are saved inducing a 
  disconnect then reconnect, if a connection can not be established, the module will attempt to establish a connection 
  every 60 seconds.
    - In `v0.0.2`, Hydra-MQTT required a manual module restart if a connection could not be established on startup.

- __Progressively More Aggressive Reconnection__: If the connection to a broker is lost, Hydra-MQTT will attempt to reconnect
  in a progressively more aggressive manner.
    - 1) _Soft Reconnect_: First, Hydra-MQTT will attempt to reconnect using the same MQTT client and session.
    - 2) _Hard Reconnect_: Second, Hydra-MQTT will attempt to reconnect using the same MQTT client, but a new MQTT session.
    - 3) _Initialize Reconnection_: Third, Hydra-MQTT will attempt to reconnect using a new MQTT client and session.
    - 4) _Repeat_: These three steps will be repeated every 60 seconds until a connection is restored.
    - Depending on whether a new client and/or session was used to reconnect, retained broker messages may be received
      by Hydra-MQTT.

- __Data Type Support__: All Ignition data types are now supported except `Binary Data`.
    - `User Defined Types (UDTs)` are not currently supported.
    - See the [Data Types](Pages/Getting%20Started/03-Data%20Types.md) section.
    - In `v0.0.2`, a limited number of data types were supported.

- __Multiple Subscribe Paths__: Multiple subscribe `Value Path`s and `Timestamp Path`s can be specified.
    - This allows Hydra-MQTT to ingest multiple unique representations.
    - See the [Representation](Pages/Configuration/06-Representation.md#subscribe) section.

- __Do Not Publish On Tag Delete__: Hydra-MQTT will not publish a value when a tag is deleted.
    - In `v0.0.2`, when a tag was deleted, Hydra-MQTT would publish a `null` value.

- __Experimental: Global Subscription Mode__: This mode uses a new strategy for responding to tag changes.
    - By default, this option is disabled.
    - This affects how Hydra-MQTT responds to tag changes '_Under-The-Hood_'.
    - See the [Experimental](Pages/Configuration/07-Experimental.md) section.


## v0.0.2

- __Publish__: Publish changes to Ignition tags to a broker.

- __Publish Timestamp Format__: Specify what format `Timestamp`s should be published in.
    - See the [Representation](Pages/Configuration/06-Representation.md#timestamp-format) section.

- __Subscribe Timestamp Integer Format__: Specify how integer `Timestamp`s should be interpreted during ingestion.
    - See the [Representation](Pages/Configuration/06-Representation.md#timestamp-integer-format) section.

## v0.0.1

- __Subscribe__: Subscribe to a broker and create or modify Ignition tags.
