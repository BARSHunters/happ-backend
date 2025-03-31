# Social Service

The Social Service is a microservice that handles user interactions in a social network application. It provides
functionality for managing friendships, friend requests, and user profiles.

## Features

- Send friend requests
- Respond to friend requests (accept/reject)
- View user profiles with friend lists
- Real-time notifications for friend-related events

## Database Setup

The service requires a PostgreSQL database with the following table:

```sql
CREATE TABLE friendships
(
    id                UUID PRIMARY KEY,
    sender_username   VARCHAR(255) NOT NULL,
    receiver_username VARCHAR(255) NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL
);
```

## Building and Running

1. Build the project:

```bash
./gradlew build
```

2. Run the service:

```bash
./gradlew run
```

## API Endpoints

The service communicates through Redis channels:

### Friend Requests

- Send friend request:
    - Channel: `social:request:ProposeFriendship`
    - Request body:
      ```json
      {
        "id": "uuid",
        "dto": {
          "senderUsername": "string",
          "receiverUsername": "string"
        }
      }
      ```
    - Response channel: `social:response:ProposeFriendship`

- Respond to friend request:
    - Channel: `social:request:RespondToFriendship`
    - Request body:
      ```json
      {
        "id": "uuid",
        "dto": {
          "senderUsername": "string",
          "receiverUsername": "string",
          "response": "accept|reject"
        }
      }
      ```
    - Response channel: `social:response:RespondToFriendship`

### User Profiles

- Get user profile:
    - Channel: `social:request:GetUserProfile`
    - Request body:
      ```json
      {
        "id": "uuid",
        "dto": "username"
      }
      ```
    - Response channel: `social:response:GetUserProfile`

## Error Handling

The service returns errors through the `error` channel with the following format:

```json
{
  "id": "uuid",
  "error": {
    "errorType": "BAD_REQUEST|INTERNAL_SERVER_ERROR",
    "errorMessage": "string"
  }
}
```

## Testing

Run the tests:

```bash
./gradlew test
``` 