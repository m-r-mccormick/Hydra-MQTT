---
title: 'Overview'
---

# Demo

[Real-Time Manufacturing Datasets](https://github.com/m-r-mccormick/Real-Time-Manufacturing-Datasets){target=_blank} is a educational resource which utilizes this module and includes pre-configured tooling (i.e., docker-compose stacks) and manufacturing datasets. As such, it is a useful resource for learning to use this module.

# Tag Providers

This module uses two distinct tag providers, one dedicated to publishing tag changes to the broker (`Publish-Only Tag Provider`), and another for subscribing to changes in the broker (`Subscribe-Only Tag Provider`).

The `Subscribe-Only Tag Provider` is automatically populated with tags. The path to each tag (i.e., `Tag Path`) is derived from the `Topic` of the received payload. By extension, parent folders are automatically created in the `Subscribe-Only Tag Provider` to house the created tags.

Conversely, the `Publish-Only Tag Provider` must be manually constructed by adding folders and tags. However, any changes made to tags in the `Publish-Only Tag Provider` are automatically published to the broker. In addition, the `Topic` which the change is published to is derived from the `Tag Path` in the `Publish-Only Tag Provider`.

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

# Data Representations

This module only supports payloads which are in JavaScript Object Notation (JSON) format. Payloads __must__ contain a `Value`, and __may__ contain a `Timestamp` (depending on configuration options). The location of each within a payload is configurable via a `Value Path` and `Timestamp Path`. 

???+ example "Flat Example"

	```bash title='Configuration'
	Value_Path="Value"
	Timestamp_Path="Timestamp"
	```
	<div class="result">
	```json title='Payload'
	{
		"Value": 42,
		"Timestamp": "2025-01-01T00:00:00.000+00:00"
	}
	```
	</div>

???+ example "Nested Example"

	```bash title='Configuration'
	Value_Path="Observation.Value"
	Timestamp_Path="Observation.Timestamp"
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Value": 42,
			"Timestamp": "2025-01-01T00:00:00.000+00:00"
		}
	}
	```
	</div>
	
??? example "Mixed Example"

	```bash title='Configuration'
	Value_Path="Value"
	Timestamp_Path="Observation.Timestamp"
	```
	<div class="result">
	```json title='Payload'
	{
		"Value": 42
		"Observation": {
			"Timestamp": "2025-01-01T00:00:00.000+00:00"
		}
	}
	```
	</div>

These values are indepdently configurable for both publishing and subscribing. While the `Publish Timestamp Path` is required and is always published, the `Subscribe Timestamp Path` is optional. If the `Subscribe Timestamp Path` is specified and is not present in a received payload, the payload is __silently discarded__. If the `Subscribe Timestamp Path` is not specified, the time of receipt of a payload is assigned as the `Timestamp`, and whether the payload contains a `Timestamp` is disregarded. So, If the `Subscribe Timestamp Path` is not specified, as long as a `Value` is present at the `Subscribe Value Path`, the associated `Tag` will be updated accordingly.

