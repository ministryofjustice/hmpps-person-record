# 1. Use jsonpath to add cprUUID to Court Messages

## Status

Accepted

## Context

Court messages require the cprUUID (Core Person Record unique identifier) for each defendant so that any applications consuming court messages can call the Core Person Record API to retrieve defendant data. The remainder of the message must be sent on exactly as it is received, including message attributes.

### Solutions considered

#### jsonpath

##### Summary

##### For

- Smaller change to the project
- Quick to implement

##### Against

- The project has not been updated for over a year, although this is perhaps because it is very simple and stable and has very few dependencies
- The jsonpath expression used to add cprUUID is hard to understand and will break if the JSON structure changes in this area

#### jackson objects

##### Summary

##### For

- The technology is already in use in the project
- We could reuse the jackson objects used in [Court Hearing Event Receiver](https://github.com/ministryofjustice/court-hearing-event-receiver)
- Pure Kotlin solution is possible

##### Against

- We would have to add a lot of new objects which have no relevance to this project and would simply be used for serialising and deserialising
- We would have to keep up with changes to these objects in Court Hearing Event Receiver (although these are not frequent)

## Decision

We will use jsonpath to read the original message payload and insert the correct cprUUID for each defendant.

## Consequences

We add a dependency on [jsonpath](https://github.com/json-path/JsonPath)