---
title: 'Broker'
---

# Host

This option specifies the `IP Address` or `DNS Name` of the MQTT broker.

| Location | Type | Provider | Address |
| -------- | ---- | -------- | ------- |
| Local Network | `IP Address` | You | `192.168.1.105` |
| Internet | `DNS Name` | Eclipse | `mqtt.eclipse.org` |
| Internet | `DNS Name` | Mosquitto | `test.mosquitto.org` |
| Internet | `DNS Name` | HiveMQ | `broker.hivemq.com` |
| Docker Network | `DNS Name` | [Real-Time Manufacturing Datasets](https://github.com/m-r-mccormick/Real-Time-Manufacturing-Datasets){ target=_blank } | `broker` |

???+ example

	Using [Real-Time Manufacturing Datasets](https://github.com/m-r-mccormick/Real-Time-Manufacturing-Datasets){ target=_blank }:
	
	```text title="Host"
	broker
	```

# Port

This option specifies the `Port` that the broker service is using on the host. While not a standard, a convention is that brokers with no security typically use port `1883`, while brokers using Transport Layer Security (TLS) typically use port `8883`.

| Type | Port |
| ---- | ---- |
| No Security | `1883` |
| TLS | `8883` |

???+ info

    TLS is not currently supported.

???+ example

	```text title="Port"
	1883
	```

# Publish QoS

This option specifies the Quality of Service (QoS) level for data published to the broker. Higher QoS levels can improve data integrity at the cost of performance.

| Level | Meaning |
| ----- | ------- |
| 0 | At Most Once |
| 1 | At Least Once |
| 2 | Exactly Once |

???+ example

	```text title="Publish QoS"
	0
	```

# Subscribe QoS

This option specifies the Quality of Service (QoS) level for subscriptions to (i.e., data received from) the broker. Higher QoS levels can improve data integrity at the cost of performance.

| Level | Meaning |
| ----- | ------- |
| 0 | At Most Once |
| 1 | At Least Once |
| 2 | Exactly Once |

???+ example
	
	```text title="Subscribe QoS"
	0
	```

# Subscriptions

This option specifies broker subscriptions, with one subscription per line.

???+ example

	```text title="Subscribe to All Broker Data (Excluding $SYS)"
	#
	```

	```text title="Subscribe to All Subtopics of 'A/a/' and 'B/b/'"
	A/a/#
	B/b/#
	```

