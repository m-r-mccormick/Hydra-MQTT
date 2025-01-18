---
title: 'Payload Structure'
---

Payloads __must__ contain a `Value`, and __may__ contain a `Timestamp` (depending on configuration options). The location of each within a payload is configurable via a `Value Path` and `Timestamp Path`. 

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

These values are independently configurable for both publishing and subscribing. While the `Publish Timestamp Path` is required and is always published, the `Subscribe Timestamp Path` is optional. If the `Subscribe Timestamp Path` is specified and is not present in a received payload, the payload is __silently discarded__. If the `Subscribe Timestamp Path` is not specified, the time of receipt of a payload is assigned as the `Timestamp`, and whether the payload contains a `Timestamp` is disregarded. So, If the `Subscribe Timestamp Path` is not specified, as long as a `Value` is present at the `Subscribe Value Path`, the associated `Tag` will be updated accordingly.


