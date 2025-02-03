---
title: 'Representation'
---

# Publish

## Value Path

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
	
??? example "Nested Example"

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

## Timestamp Path

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
	
??? example "Nested Example"

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

## Timestamp Format

This option specifies the format that `Timestamp`s should be published in.

| Format | Json Type | Output |
| ------ | --------- | ------ |
| ISO8601 | String | "2025-01-01T00:00:00.000+00:00" |
| DoubleUnixEpochSeconds | Double | 1735707600.000000000 |
| IntegerUnixEpochSeconds | Integer | 1735707600 |
| IntegerUnixEpochNanoseconds | Integer | 1735707600000000000 |

???+ warning

	The setting only affects the format of `Timestamp`s, and not `Value`s which have an ignition type of `Date` 
    or `Date Array`. `Date` and `Date Array` `Value`s are always ISO-8601 encoded.

???+ example "ISO8601 Example"

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
	
??? example "DoubleUnixEpochSeconds Example"

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
	
??? example "IntegerUnixEpochSeconds Example"

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
	
??? example "IntegerUnixEpochNanoseconds Example"

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

## Documentation Path

This option specifies the location within a json payload where the `Documentation` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload.

???+ example

	```bash title='Publish Documentation Path'
	Documentation
	```
	<div class="result">
	```json title='Payload'
	{
		"Documentation": "My Documentation"
	}
	```
	</div>

??? example "Nested Example"

	```bash title='Publish Timestamp Path'
	Observation.Documentation
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Documentation": "My Documentation"
		}
	}
	```
	</div>

## Engineering Units Path

This option specifies the location within a json payload where the `Engineering Units` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload.

???+ example

	```bash title='Publish Units Path'
	Units
	```
	<div class="result">
	```json title='Payload'
	{
		"Units": "Freedoms Per Eagle"
	}
	```
	</div>

??? example "Nested Example"

	```bash title='Publish Timestamp Path'
	Observation.Units
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Units": "Freedoms Per Eagle"
		}
	}
	```
	</div>


# Subscribe

## Value Paths

This option specifies multiple locations within a json payload where a `Value` should be located, with a period (`.`) 
delimiting levels (i.e., dictionaries) within the payload. If a payload is received which is 1) not 
deserializable json or 2) does not contain a `Value` at the specified location, the payload is __silently discarded__.

Each line is a separate path. The specified paths are iterated through from top to bottom, and the first path which
contains a value is used.

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
    ```json title='Result'
	42
	```
	</div>

??? example "Nested Example"

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
    ```json title='Result'
	42
	```
	</div>

??? example "Multiple Value Paths Example"

	```bash title='Subscribe Value Path'
	Observation.Value
    Value
    Observation.Nested.Value
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Value": 42,
            "Nested": {
                "Value": 3.125
            }
		}
        "Value": "Panda",
	}
	```
    ```json title='Result'
	42
	```
	</div>

## Timestamp Paths

This option specifies the multiple location within a json payload where the `Timestamp` should be, with a period (`.`) 
delimiting levels (i.e., dictionaries) within the payload.

Each line is a separate path. The specified paths are iterated through from top to bottom, and the first path which
contains a timestamp is used.

If `Subscribe Timestamp Paths` is defined and a payload is received which is 1) not deserializable json or 2) does not contain the `Timestamp` at the specified location, the payload is __silently discarded__.

If `Subscribe Timestamp Paths` is not defined, a `Timestamp` is not checked for in received payloads, and the current time is assigned as the `Timestamp` in stead. However, a `Value` at the `Subscribe Value Path` must still be found.

???+ warning "Future Timestamps"

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
    ```json title='Result'
	"2025-01-01T00:00:00.000+00:00"
	```
	</div>

??? example "Nested Example"

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
    ```json title='Result'
	"2025-01-01T00:00:00.000+00:00"
	```
	</div>

??? example "Multiple Timestamp Paths Example"

	```bash title='Subscribe Timestamp Path'
	Observation.Timestamp
    Timestamp
    Observation.Nested.Timestamp
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Timestamp": "2025-01-01T00:00:00.000+00:00"
            "Nested": {
                "Timestamp": 1735707600000000000
            }
		}
        "Timestamp": 1735707600.000000000
	}
	```
    ```json title='Result'
	"2025-01-01T00:00:00.000+00:00"
	```
	</div>

## Timestamp Integer Format

When decoding a `Timestamp`, strings are assumed to be ISO8601 format, and doubles are assumed to be seconds since the Unix Epoch (January 1, 1970). However, two formats/precisions are commonly represented by integers:

1. Seconds since the Unix Epoch: `UnixEpochSeconds`
2. Nanoseconds since the Unix Epoch `UnixEpochNanoseconds`

This option specifies which format integer `Timestamp`s should be decoded as.

| Json Type | Input                           | Assumed Format/Precision                           |
|-----------|---------------------------------|----------------------------------------------------|
| String    | "2025-01-01T00:00:00.000+00:00" | ISO-8601                                           |
| Double    | 1735707600.000000000            | DoubleUnixEpochSeconds                             |
| Integer   | 1735707600                      | :fontawesome-solid-triangle-exclamation: `Unknown` |
| Integer   | 1735707600000000000             | :fontawesome-solid-triangle-exclamation: `Unknown` |

???+ warning

	The setting only affects the format of `Timestamp`s, and not `Value`s which have an ignition type of `Date` 
    or `Date Array`. `Date` and `Date Array` `Value`s are always ISO-8601 encoded.

??? example "UnixEpochSeconds Example"

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
	
??? example "UnixEpochNanoseconds Example"

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

## Documentation Paths

This option specifies the multiple location within a json payload where the `Documentation` should be, with a period (`.`)
delimiting levels (i.e., dictionaries) within the payload.

Each line is a separate path. The specified paths are iterated through from top to bottom, and the first path which
contains `Documentation` is used.

If `Subscribe Documentation Paths` is defined and is not identified in a received payload, the payload is still
consumed and missing `Documentation` is silently ignored.

If `Subscribe Documentation Paths` is not defined, `Documentation` is not checked for in received payloads.

???+ example

	```bash title='Subscribe Documentation Paths'
	Documentation
	```
	<div class="result">
	```json title='Payload'
	{
		"Documentation": "My Documentation"
	}
	```
	</div>

??? example "Nested Example"

	```bash title='Subscribe Documentation Paths'
	Observation.Documentation
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Documentation": "My Documentation"
		}
	}
	```
	</div>

??? example "Multiple Documentation Paths Example"

	```bash title='Subscribe Documentation Paths'
	Observation.Documentation
    Documentation
    Observation.Nested.Documentation
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Documentation": "My Documentation",
            "Nested": {
                "Documentation": "Flux Capacitor Core Temperature"
            }
		}
        "Documentation": "Left Hand Threads Only"
	}
	```
    ```json title='Result'
	My Documentation
	```
	</div>

## Engineering Units Paths

This option specifies the multiple location within a json payload where the `Engineering Units` should be, with a period (`.`)
delimiting levels (i.e., dictionaries) within the payload.

Each line is a separate path. The specified paths are iterated through from top to bottom, and the first path which
contains `Units` is used.

If `Subscribe Units Paths` is defined and is not identified in a received payload, the payload is still
consumed and missing `Units` is silently ignored.

If `Subscribe Units Paths` is not defined, `Units` is not checked for in received payloads.


This option specifies the location within a json payload where the `Engineering Units` should be, with a period (`.`) delimiting levels (i.e., dictionaries) within the payload.

???+ example

	```bash title='Subscribe Units Paths'
	Units
	```
	<div class="result">
	```json title='Payload'
	{
		"Units": "Freedoms Per Eagle"
	}
	```
	</div>

??? example "Nested Example"

	```bash title='Subscribe Units Paths'
	Observation.Units
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Units": "Freedoms Per Eagle"
		}
	}
	```
	</div>

??? example "Multiple Units Paths Example"

	```bash title='Subscribe Units Paths'
	Observation.Units
    Units
    Observation.Nested.Units
	```
	<div class="result">
	```json title='Payload'
	{
		"Observation": {
			"Units": "Freedoms Per Eagle",
            "Nested": {
                "Units": "Furlongs Per Fortnight"
            }
		}
        "Units": "Hampsterwatt Microcenturies"
	}
	```
    ```json title='Result'
	Freedoms Per Eagle
	```
	</div>

