---
title: 'Tag Providers'
---

As demonstrated in the tutorial, Hydra-MQTT uses two distinct tag providers, one dedicated to publishing tag changes to the broker (`Publish-Only Tag Provider`), and another for subscribing to changes in the broker (`Subscribe-Only Tag Provider`).

The `Subscribe-Only Tag Provider` is automatically populated with tags. The path to each tag (i.e., `Tag Path`) is derived from the `Topic` of the received payload. By extension, parent folders are automatically created in the `Subscribe-Only Tag Provider` to house the created tags.

Conversely, the `Publish-Only Tag Provider` must be manually constructed by adding folders and tags. Any changes made to tags in the `Publish-Only Tag Provider` are automatically published to the broker. In addition, the `Topic` which the change is published to is derived from the `Tag Path` in the `Publish-Only Tag Provider`.

Consequently, the `Publish-Only Tag Provider`, broker, and `Subscribe-Only Tag Provider` inherently share the same structure, which is illustrated using the ISA95 hierarchy of `Enterprise`, `Site`, `Area`, `Line`, `Cell`, `...` below:

<div class="grid cards custom" markdown>

-   :fontawesome-solid-tag:{ .middle } __Publish-Only<br>Tag Provider__{.lg .middle}

    ---

    ```yaml
    [Pub-Only Root]
    └─E1: [Folder]
    │ └─S1: [Folder]
    │ │ └─A1: [Folder]
    │ │ │ └─L1: [Folder]
    │ │ │ │ └─C1: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    │ │ │ │ └─C2: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    │ │ │ └─L2: [Line]
    │ │ │ │ └─C1: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    │ │ │ │ └─C2: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    ```
    
-   :fontawesome-solid-circle-nodes:{ .middle } __MQTT Broker__{.lg .middle}

    ---

    ```yaml
    [MQTT Topic Root]
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

-   :fontawesome-solid-tag:{ .middle } __Subscribe-Only<br>Tag Provider__{.lg .middle}

    ---

    ```yaml
    [Sub-Only Root]
    └─E1: [Folder]
    │ └─S1: [Folder]
    │ │ └─A1: [Folder]
    │ │ │ └─L1: [Folder]
    │ │ │ │ └─C1: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    │ │ │ │ └─C2: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    │ │ │ └─L2: [Line]
    │ │ │ │ └─C1: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    │ │ │ │ └─C2: [Folder]
    │ │ │ │ │ └─V1: [Tag]
    │ │ │ │ │ └─V2: [Tag]
    ```

</div>

<div class="grid cards" markdown>

-   :fontawesome-solid-arrow-right:{ .middle } __Publish-Only Tag Provider<br>To Broker__{.lg .middle}

    ---

    | Tag Path | Topic |
	| -------- | ----- |
	| `E1/S1/A1/L2/C1/V2` | `E1/S1/A1/L2/C1/V2` |
    
-   :fontawesome-solid-arrow-right:{ .middle } __Broker To<br>Subscribe-Only Tag Provider__{.lg .middle}

    ---

    | Topic | Tag Path |
	| -------- | ----- |
	| `E1/S1/A1/L2/C1/V2` | `E1/S1/A1/L2/C1/V2` |

</div>
