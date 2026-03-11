# REQUIREMENTS file (Immutable)
**This file is the project mission and must not be changed.**
**If this file is changed by the user, you are NEVER to restore it via git restore, and always accept the current version as-is**

# Requirements
Backend application that will simulate sports betting event outcome handling and bet settlement via Kafka and RocketMQ.
- An API endpoint to publish a sports event outcome to Kafka.
- A Kafka consumer that listens to event-outcomes Kafka named-topic.
- Matches the event outcome to bets that need to be settled.
- A RocketMQ producer that sends messages to bet-settlements.

# Sports Betting Settlement Trigger Service
We want to launch a new backend service that will simulate sports betting event outcome handling and bet settlement via Kafka and RocketMQ. Relevant Use Cases to support:
## 1. An API endpoint to publish a sports event outcome to Kafka.
### a. This endpoint should publish an event outcome to Kafka. An event outcome is represented by:
i. Event ID
ii. Event Name
iii. Event Winner ID
### b. The topic should be event-outcomes as commented before.

## 2. A Kafka consumer that listens to event-outcomes Kafka named-topic.

## 3. Matches the event outcome to bets that need to be settled.
### a. The System checks if we have bets in our database that can be settled based on Event ID from the event outcome message.
### b. A bet in database is composed by:
i. Bet ID
ii. User ID
iii. Event ID
iv. Event Market ID
v. Event Winner ID
vi. Bet Amount
## 4. A RocketMQ producer that sends messages to bet-settlements.
### a. After identifying the bets to be settled in the previous point, here we should produce the messages for RocketMQ so the bet can be settled.

# Conditions:
- Use an in-memory database for the bets.
