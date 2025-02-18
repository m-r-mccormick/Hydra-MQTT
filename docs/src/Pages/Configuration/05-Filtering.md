---
title: 'Filtering'
---

# Publish Topic

## Include Regex Match Any

This option specifies a collection of regex patterns which are used to filter events based on topic, publishing only
topics which match at least one of the specified patterns.

If this option is empty, no filtering is executed, and all events are passed downstream.

???+ example "Only publish topics which begin with `InGenLabs`"

	```bash title='Patterns'
	^InGenLabs/.*
	```

## Exclude Regex Match Any

This option specifies a collection of regex patterns which are used to filter events based on topic, __not__ publishing
topics which match at least one of the specified patterns.

If this option is empty, no filtering is executed, and all events are passed downstream.

???+ example "Do not publish topics which begin with `InGenLabs`"

	```bash title='Patterns'
	^InGenLabs/.*
	```

# Subscribe Topic

## Include Regex Match Any

This option specifies a collection of regex patterns which are used to filter events based on topic, receiving only
topics which match at least one of the specified patterns.

If this option is empty, no filtering is executed, and all events are passed downstream.

???+ example "Only receive topics which begin with `InGenLabs`"

	```bash title='Patterns'
	^InGenLabs/.*
	```

## Exclude Regex Match Any

This option specifies a collection of regex patterns which are used to filter events based on topic, __not__ receiving
topics which match at least one of the specified patterns.

If this option is empty, no filtering is executed, and all events are passed downstream.

???+ example "Do not receive topics which begin with `InGenLabs`"

	```bash title='Patterns'
	^InGenLabs/.*
	```
