---
title: 'Representation'
---

# Publish Value Path

This option specifies the location within a json payload where the `Value` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload.

???+ example

	```bash title='Publish Value Path'
	Value
	```
	<div class="result">
	```json title='Payload'
	{
		"Value": 42
	}
	```
	</div>
	
???+ example

	```bash title='Publish Value Path'
	Observation.Value
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Value": 42
		}
	}
	```
	</div>

# Publish Timestamp Path

This option specifies the location within a json payload where the `Timestamp` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload.

???+ example

	```bash title='Publish Timestamp Path'
	Timestamp
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": "2025-01-01T00:00:00.000+00:00"
	}
	```
	</div>
	
???+ example

	```bash title='Publish Timestamp Path'
	Observation.Timestamp
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Timestamp": "2025-01-01T00:00:00.000+00:00"
		}
	}
	```
	</div>

# Publish Timestamp Format

This option specifies the format that `Timestamp`s should be published in.

| Format | Json Type | Output |
| ------ | --------- | ------ |
| ISO8601 | String | "2025-01-01T00:00:00.000+00:00" |
| DoubleUnixEpochSeconds | Double | 1735707600.000000000 |
| IntegerUnixEpochSeconds | Integer | 1735707600 |
| IntegerUnixEpochNanoseconds | Integer | 1735707600000000000 |

???+ example

	```bash title='Publish Timestamp Format'
	ISO8601
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": "2025-01-01T00:00:00.000+00:00"
	}
	```
	</div>
	
???+ example

	```bash title='Publish Timestamp Format'
	DoubleUnixEpochSeconds
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": 1735707600.000000000
	}
	```
	</div>
	
???+ example

	```bash title='Publish Timestamp Format'
	IntegerUnixEpochSeconds
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": 1735707600
	}
	```
	</div>
	
???+ example

	```bash title='Publish Timestamp Format'
	IntegerUnixEpochNanoseconds
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": 1735707600000000000
	}
	```
	</div>

# Subscribe Value Path

This option specifies the location within a json payload where the `Value` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload. If a payload is received which is 1) not deserializable json or 2) does not contain the `Value` at the specified location, the payload is __silently discarded__.

???+ example

	```bash title='Subscribe Value Path'
	Value
	```
	<div class="result">
	```json title='Payload'
	{
		"Value": 42
	}
	```
	</div>
	
???+ example

	```bash title='Subscribe Value Path'
	Observation.Value
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Value": 42
		}
	}
	```
	</div>

# Subscribe Timestamp Path

This option specifies the location within a json payload where the `Timestamp` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload.

If `Subscribe Timestamp Path` is defined and a payload is received which is 1) not deserializable json or 2) does not contain the `Timestamp` at the specified location, the payload is __silently discarded__.

If `Subscribe Timestamp Path` is not defined, a `Timestamp` is not checked for in received payloads, and the current time is assigned as the `Timestamp` in stead. However, a `Value` at the `Subscribe Value Path` must still be found.

???+ info "Future Timestamps"

    The Ignition SDK automatically changes any future timestamps (i.e., timestamps which indicate a time in the future)
    to the current time. As a result, if `Subscribe Timestamp Path` is set and a payload contains a future timestamp,
    the timestamp will be __silently__ adjusted to the time that the payload was received.

???+ example

	```bash title='Subscribe Timestamp Path'
	Timestamp
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": "2025-01-01T00:00:00.000+00:00"
	}
	```
	</div>
	
???+ example

	```bash title='Subscribe Timestamp Path'
	Observation.Timestamp
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Timestamp": "2025-01-01T00:00:00.000+00:00"
		}
	}
	```
	</div>

# Subscribe Timestamp Integer Format

When decoding a `Timestamp`, strings are assumed to be ISO8601 format, and doubles are assumed to be seconds since the Unix Epoch (January 1, 1970). However, two formats/precisions are commonly represented by integers:

1. Seconds since the Unix Epoch: `UnixEpochSeconds`
2. Nanoseconds since the Unix Epoch `UnixEpochNanoseconds`

This option specifies which format integer `Timestamp`s should be decoded as.

| Json Type | Input | Assumed Format/Precision |
| --------- | ----- | ------------------------ |
| String | "2025-01-01T00:00:00.000+00:00" | ISO8601 |
| Double | 1735707600.000000000 | DoubleUnixEpochSeconds |
| Integer | 1735707600 | :fontawesome-solid-triangle-exclamation: `Unknown` |
| Integer | 1735707600000000000 | :fontawesome-solid-triangle-exclamation: `Unknown` |

???+ example

	```bash title='Subscribe Timestamp Integer Format'
	UnixEpochSeconds
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": 1735707600
	}
	```
	</div>
	
???+ example

	```bash title='Subscribe Timestamp Integer Format'
	UnixEpochNanoseconds
	```
	<div class="result">
	```json title='Payload'
	{
		"Timestamp": 1735707600000000000
	}
	```
	</div>

