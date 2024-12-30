---
title: 'Routing'
---


# Settings

## Publish Topic Suffix

This option:

1. Appends the specified suffix to the `Topic` when publishing `Tag` changes.
2. Ignores received events from `Topic`s with the specified suffix.

This is intended help prevent feedback loops that may occur when using a `PubSub Tag Provider`, or to help prevent a `Subscribe-Only Tag Provider` from receiving data published by a `Publish-Only Tag Provider`.

???+ example

	```text title="Publish Topic Suffix"
	_write
	```
