---
title: 'Tag Provider'
---




# Publish-Only Tag Provider

This setting specifies the `Name` of a deciated `Realtime Tag Provider` defined in `Config > Tags > Realtime`. Changes to a `Tag` in the specified `Realtime Tag Provider` will be published to the broker, but any events received from the broker will not result in `Tag` changes. As such, this `Tag Provider` is _Publish Only_.

When a `Tag` in the specified `Realtime Tag Provider` changes, the `Tag Path` (i.e., folder hierarchy) is translated into an equivalent `Topic`. For example a `Tag` with the following `Tag Path` would publish to the specified `Topic`:

| Tag Path | Topic |
| -------- | ----- |
| `e1/s1/a1/l2/c1/v2` | `E1/S1/A1/L2/C1/V2` |

<div class="grid cards" markdown>

-   :fontawesome-solid-tag:{ .middle } __Ignition Tag Provider__{.lg .middle} :fontawesome-solid-arrow-right:{ .middle }

    ---

    ```yaml
    [Folder]
    └─e1: [Folder]
    │ └─s1: [Folder]
    │ │ └─a1: [Folder]
    │ │ │ └─l1: [Folder]
    │ │ │ │ └─c1: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    │ │ │ │ └─c2: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    │ │ │ └─l2: [Line]
    │ │ │ │ └─c1: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    │ │ │ │ └─c2: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    ```
    
-   :fontawesome-solid-arrow-right:{ .middle } :fontawesome-solid-circle-nodes:{ .middle } __MQTT Broker__{.lg .middle}

    ---

    ```yaml
    [Tag Provider Root]
    └─E1: [Enterprise]
    │ └─S1: [Site]
    │ │ └─A1: [Area]
    │ │ │ └─L1: [Line]
    │ │ │ │ └─C1: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    │ │ │ │ └─C2: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    │ │ │ └─L2: [Line]
    │ │ │ │ └─C1: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    │ │ │ │ └─C2: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    ```

</div>

# Subscribe-Only Tag Provider

This setting specifies the `Name` of a deciated `Realtime Tag Provider` defined in `Config > Tags > Realtime`. An event received from the broker results in a `Tag` change in the specified `Realtime Tag Provider`, but any changes to `Tag`s will not result in changes being published to the broker. As such, this `Tag Provider` is _Subscribe Only_.

When a broker event is received, the `Topic` of the event is translated into an equivalent `Tag Path`. For example, an event with the following `Topic` will be translated to the equivalent `Tag Path` resulting in that `Tag` being modified:

| Topic | Tag Path |
| -------- | ----- |
| `E1/S1/A1/L2/C1/V2` | `e1/s1/a1/l2/c1/v2` |

If a `Tag` with the specified `Tag Path` does not exist, it will be automatically created, along with all parent `Folder`s. If a `Folder` already exists at the `Tag Path`, the event will be __silently dropped__.

<div class="grid cards" markdown>

-   :fontawesome-solid-circle-nodes:{ .middle } __MQTT Broker__{.lg .middle} :fontawesome-solid-arrow-right:{ .middle }

    ---

    ```yaml
    [Tag Provider Root]
    └─E1: [Enterprise]
    │ └─S1: [Site]
    │ │ └─A1: [Area]
    │ │ │ └─L1: [Line]
    │ │ │ │ └─C1: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    │ │ │ │ └─C2: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    │ │ │ └─L2: [Line]
    │ │ │ │ └─C1: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    │ │ │ │ └─C2: [Cell]
    │ │ │ │ │ └─V1: [Variable]
    │ │ │ │ │ └─V2: [Variable]
    ```

-   :fontawesome-solid-arrow-right:{ .middle } :fontawesome-solid-tag:{ .middle } __Ignition Tag Provider__{.lg .middle}

    ---

    ```yaml
    [Folder]
    └─e1: [Folder]
    │ └─s1: [Folder]
    │ │ └─a1: [Folder]
    │ │ │ └─l1: [Folder]
    │ │ │ │ └─c1: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    │ │ │ │ └─c2: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    │ │ │ └─l2: [Line]
    │ │ │ │ └─c1: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    │ │ │ │ └─c2: [Folder]
    │ │ │ │ │ └─v1: [Tag]
    │ │ │ │ │ └─v2: [Tag]
    ```

</div>
