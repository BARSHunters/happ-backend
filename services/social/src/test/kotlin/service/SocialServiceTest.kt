package service

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import keydb.sendEvent
import model.Friendship
import model.FriendshipStatus
import model.request.FriendshipRequestDto
import model.response.FriendshipResponseDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import repository.FriendshipRepository
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SocialServiceTest {
    private lateinit var socialService: SocialService
    private lateinit var friendshipRepository: FriendshipRepository
    private lateinit var userProfileService: UserProfileService

    @BeforeEach
    fun setup() {
        friendshipRepository = mockk(relaxUnitFun = true)
        userProfileService = mockk(relaxUnitFun = true)
        mockkStatic(::sendEvent)
        every { sendEvent(any(), any()) } returns Unit
        socialService = SocialService(friendshipRepository, userProfileService)
    }

    @Test
    fun testProposeFriendship() {
        // Arrange
        val request = FriendshipRequestDto(
            senderUsername = "user1",
            receiverUsername = "user2"
        )
        every { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) } returns null
        every { friendshipRepository.createFriendship(request.senderUsername, request.receiverUsername) } returns true

        // Act
        val result = socialService.proposeFriendship(UUID.randomUUID(), request)

        // Assert
        assertTrue(result)
        verify(exactly = 1) { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) }
        verify(exactly = 1) { friendshipRepository.createFriendship(request.senderUsername, request.receiverUsername) }
        verify(exactly = 1) { sendEvent(any(), any()) }
    }

    @Test
    fun testRespondToFriendship() {
        // Arrange
        val request = FriendshipResponseDto(
            senderUsername = "user1",
            receiverUsername = "user2",
            response = "accept"
        )
        val friendshipId = UUID.randomUUID()
        val mockFriendship = mockk<Friendship> {
            every { this@mockk.id } returns friendshipId
            every { status } returns FriendshipStatus.PENDING
        }

        every { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) } returns mockFriendship
        every { friendshipRepository.updateFriendshipStatus(friendshipId, FriendshipStatus.ACCEPTED) } returns true

        // Act
        val result = socialService.respondToFriendship(UUID.randomUUID(), request)

        // Assert
        assertTrue(result)
        verify(exactly = 1) { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) }
        verify(exactly = 1) { friendshipRepository.updateFriendshipStatus(friendshipId, FriendshipStatus.ACCEPTED) }
        verify(exactly = 1) { sendEvent(any(), any()) }
    }

    @Test
    fun testGetFriendsList() {
        // Arrange
        val username = "user1"
        val friends = listOf("friend1", "friend2", "friend3")
        every { friendshipRepository.getFriends(username) } returns friends

        // Act
        val result = socialService.getFriendsList(username)

        // Assert
        assertEquals(friends, result)
        verify(exactly = 1) { friendshipRepository.getFriends(username) }
        verify(exactly = 0) { sendEvent(any(), any()) }
    }

    @Test
    fun testGetUserProfile() {
        // Arrange
        val username = "user1"
        val friends = listOf("friend1", "friend2")
        every { friendshipRepository.getFriends(username) } returns friends

        // Act
        socialService.getUserProfile(UUID.randomUUID(), username)

        // Assert
        verify(exactly = 1) { friendshipRepository.getFriends(username) }
        verify(exactly = 1) { userProfileService.requestUserProfile(any(), eq(username), eq(friends.size)) }
    }

    @Test
    fun testProposeFriendshipWhenExists() {
        // Arrange
        val request = FriendshipRequestDto(
            senderUsername = "user1",
            receiverUsername = "user2"
        )
        every { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) } returns mockk()

        // Act
        val result = socialService.proposeFriendship(UUID.randomUUID(), request)

        // Assert
        assertFalse(result)
        verify(exactly = 1) { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) }
        verify(exactly = 0) { friendshipRepository.createFriendship(any(), any()) }
        verify(exactly = 0) { sendEvent(any(), any()) }
    }

    @Test
    fun testRespondToFriendshipWhenNotFound() {
        // Arrange
        val request = FriendshipResponseDto(
            senderUsername = "user1",
            receiverUsername = "user2",
            response = "accept"
        )
        every { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) } returns null

        // Act
        val result = socialService.respondToFriendship(UUID.randomUUID(), request)

        // Assert
        assertFalse(result)
        verify(exactly = 1) { friendshipRepository.findFriendship(request.senderUsername, request.receiverUsername) }
        verify(exactly = 0) { friendshipRepository.updateFriendshipStatus(any(), any()) }
        verify(exactly = 0) { sendEvent(any(), any()) }
    }
}
