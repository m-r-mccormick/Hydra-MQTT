---
title: 'Data Types'
---


# JSON Data Types

Hydra-MQTT uses JavaScript Object Notation (JSON), and therefore is constrained by the flexibility that it provides.
[Valid JSON data types](https://www.w3schools.com/js/js_json_datatypes.asp) used by Hydra-MQTT are:

|           Type            | Example Value  |
|:-------------------------:|:--------------:|
|         `Boolean`         | `true`/`false` |
|         `String`          |     `" "`      |
| `Number` (Floating Point) |     `0.0`      |
|    `Number` (Integer)     |      `0`       |
|          `Array`          |     `[ ]`      |
|         `Object`          |     `{ }`      |
|          `null`           |     `null`     |


# Ignition Data Types

Multiple Ignition data types correlate with the same JSON representation.
As a result, it is impossible to identify what the originating data type size was when a payload is received through a 
subscription.
To ensure that values can be utilized, the largest associated type (e.g., 64-bit) is used when creating new tags.
As a result, the publish and subscribe types can be different for the same tag path (i.e., topic), as designated by
warning icons in the `Subscribe Type` column of the table below.

The `Date` data type is a special case. While the encoding of `Timestamp`s can be modified as described in the
[Representation](../Configuration/06-Representation.md) section, the encoding of `Date` and `Date Array` `Value`s are
always ISO-8601 strings to ensure that they can be uniquely identified and distinguished from a `Double`
(Unix Epoch in Seconds), an `Integer` (Unix Epoch Seconds and Nanoseconds), and a `String` (Any string not formatted
in ISO-8601 format). Any `Value` which is a JSON `String` and can be parsed by
[OffsetDateTime.parse(string)](https://docs.oracle.com/javase/8/docs/api/java/time/OffsetDateTime.html#parse-java.lang.CharSequence-)
with a [ISO_OFFSET_DATE_TIME](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html) formatter
(i.e., matches the ISO-8601 format) will be converted to a `Date` (or `Date Array`) data type.

Likewise, the `Document` and `Dataset` datatypes are special cases and are detailed in the 
[Complex Types](#complex-data-types) section below.

The `Binary Data` type is the only Ignition data type that is not currently supported.

In addition, `User Defined Types` (UDTs) are not currently supported.

## Standard Data Types

| Publish Type    |                         JSON Representation:                         | Subscribe Type                                          |
|-----------------|:--------------------------------------------------------------------:|---------------------------------------------------------|
| `Boolean`       |                                `true`                                | `Boolean`                                               |
| `String`        |                               `"ABC"`                                | `String`                                                |
| `Double`        |                                `1.23`                                | `Double`                                                |
| `Float`         |                                `1.23`                                | `Double` :fontawesome-solid-triangle-exclamation:       |
| `Long`          |                                `123`                                 | `Long`                                                  |
| `Integer`       |                                `123`                                 | `Long` :fontawesome-solid-triangle-exclamation:         |
| `Short`         |                                `123`                                 | `Long` :fontawesome-solid-triangle-exclamation:         |
| `Byte`          |                                `123`                                 | `Long` :fontawesome-solid-triangle-exclamation:         |
| `Date`          |                  `"2025-01-01T00:00:00.000+00:00"`                   | `Date`                                                  |
| `Boolean Array` |                           `[true, false]`                            | `Boolean Array`                                         |
| `String Array`  |                           `["ABC", "abc"]`                           | `String Array`                                          |
| `Double Array`  |                            `[1.23, 9.87]`                            | `Double Array`                                          |
| `Float Array`   |                            `[1.23, 9.87]`                            | `Double Array` :fontawesome-solid-triangle-exclamation: |
| `Long Array`    |                             `[123, 987]`                             | `Long Array`                                            |
| `Integer Array` |                             `[123, 987]`                             | `Long Array` :fontawesome-solid-triangle-exclamation:   |
| `Short Array`   |                             `[123, 987]`                             | `Long Array` :fontawesome-solid-triangle-exclamation:   |
| `Byte Array`    |                             `[123, 987]`                             | `Long Array` :fontawesome-solid-triangle-exclamation:   |
| `Date Array`    | `["2025-01-01T00:00:00.000+00:00", "2025-01-01T00:00:00.000+00:00"]` | `Date Array`                                            |
| NOT SUPPORTED   |                            `Binary Data`                             | NOT SUPPORTED                                           |
| `Document`      |                              `{ ... }`                               | `Document`                                              |
| `Dataset`       |                              `{ ... }`                               | `Dataset`                                               |

## Complex Data Types

This section details the `Document` and `Dataset` data types. If you aren't sure about the finer details, try creating
what you want using Ignition Tags in the `Publish-Only Tag Provider`, see how Hydra-MQTT publishes it, then match
what Hydra-MQTT publishes.

### Document

A `Document` `Value` can be any structure and contain any values which are JSON-serializable, as listed in the table
above.

???+ example

	```bash title='Paths'
	Value_Path="Value"
    Timestamp_Path="Timestamp"
	```
	<div class="result">
	```json title='Payload'
	{
        
		"Value": {
			"MyKey": "MyValue",
            "MyNestedDocument": {
                "Key": "Value"
            }
            "NestedList": [
                1,
                2,
                "a",
                {
                    "K": 9.9
                }
            ]
		}
        "Timestamp": 1735707600.000000000
	}
	```
	</div>

### Dataset

A dataset must follow the structure shown below. 

If the `ColumnNames` object is omitted, default column names formatted `C#` where `#` is the index of the column 
are assigned. If any of the `ColumnNames` values are not a `String`, it will be __silently discarded__.

If the `ColumnTypes` object is omitted and the all the values of each column are the same type, the column types
will be automatically identified.

???+ example

    |    a    |    b   |
    |:-------:|:------:|
    | `false` | `10.1` |
    | `true`  | `0.02` |
    | `true`  | `53.2` |
    | `true`  | `1.5`  |

    ```bash title='Paths'
	Value_Path="Value"
    Timestamp_Path="Timestamp"
	```
    <div class="result">
	```json title='Payload'
    {
        "Value": {
            "ColumnNames": [
                "a",
                "b"
            ],
            "Data": [
                [
                    false,
                    true,
                    true,
                    true
                ],
                [
                    10.1,
                    0.02,
                    53.2,
                    1.5
                ]
            ],
            "ColumnTypes": [
                "class java.lang.Boolean",
                "class java.lang.Double"
            ]
        },
        "Timestamp": "2025-01-05T22:50:22.675+00:00"
    }
	```

    

    </div>


