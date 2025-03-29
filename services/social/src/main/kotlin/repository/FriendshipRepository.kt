package repository

import database.SocialDatabase
import model.Friendship
import model.FriendshipStatus
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

class FriendshipRepository {
    fun createFriendship(senderUsername: String, receiverUsername: String): Boolean {
        return try {
            SocialDatabase.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    "INSERT INTO friendships (sender_username, receiver_username, status, created_at) VALUES (?, ?, ?, ?);"
                )
                statement.setString(1, senderUsername)
                statement.setString(2, receiverUsername)
                statement.setString(3, FriendshipStatus.PENDING.toString())
                statement.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()))
                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            false
        }
    }

    fun findFriendship(senderUsername: String, receiverUsername: String): Friendship? {
        return SocialDatabase.getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "SELECT * FROM friendships WHERE (sender_username = ? AND receiver_username = ?) OR (sender_username = ? AND receiver_username = ?);"
            )
            statement.setString(1, senderUsername)
            statement.setString(2, receiverUsername)
            statement.setString(3, receiverUsername)
            statement.setString(4, senderUsername)
            val rs = statement.executeQuery()
            if (rs.next()) {
                Friendship(
                    id = UUID.fromString(rs.getString("id")),
                    senderUsername = rs.getString("sender_username"),
                    receiverUsername = rs.getString("receiver_username"),
                    status = FriendshipStatus.valueOf(rs.getString("status")),
                    createdAt = rs.getTimestamp("created_at").toLocalDateTime()
                )
            } else null
        }
    }

    fun updateFriendshipStatus(friendshipId: UUID, status: FriendshipStatus): Boolean {
        return try {
            SocialDatabase.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    "UPDATE friendships SET status = ? WHERE id = ?;"
                )
                statement.setString(1, status.toString())
                statement.setObject(2, friendshipId, java.sql.Types.OTHER)
                statement.executeUpdate() > 0
            }
        } catch (e: Exception) {
            println("DB Error: ${e.message}")
            false
        }
    }

    fun getFriends(username: String): List<String> {
        return SocialDatabase.getConnection().use { connection ->
            val statement = connection.prepareStatement(
                "SELECT CASE " +
                "WHEN sender_username = ? THEN receiver_username " +
                "ELSE sender_username " +
                "END as friend_username " +
                "FROM friendships " +
                "WHERE (sender_username = ? OR receiver_username = ?) " +
                "AND status = ?;"
            )
            statement.setString(1, username)
            statement.setString(2, username)
            statement.setString(3, username)
            statement.setString(4, FriendshipStatus.ACCEPTED.toString())
            val rs = statement.executeQuery()
            val friends = mutableListOf<String>()
            while (rs.next()) {
                friends.add(rs.getString("friend_username"))
            }
            friends
        }
    }
} 