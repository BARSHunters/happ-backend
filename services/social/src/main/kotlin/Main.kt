import controller.SocialController
import database.SocialDatabase
import keydb.runServiceListener
import repository.FriendshipRepository
import service.SocialService
import service.UserProfileService

lateinit var socialService: SocialService
lateinit var socialController: SocialController
lateinit var userProfileService: UserProfileService

fun socialServiceStartup() {
    SocialDatabase
    val friendshipRepository = FriendshipRepository()
    userProfileService = UserProfileService()
    socialService = SocialService(friendshipRepository, userProfileService)
    socialController = SocialController(socialService)
    println("Service social is running")
}

fun proposeFriendshipRequest(requestBody: String) {
    socialController.handleProposeFriendship(requestBody)
}

fun respondToFriendshipRequest(requestBody: String) {
    socialController.handleRespondToFriendship(requestBody)
}

fun getUserProfileRequest(requestBody: String) {
    socialController.handleGetUserProfile(requestBody)
}

fun handleUserDataResponse(responseBody: String) {
    userProfileService.handleUserDataResponse(responseBody)
}

fun getFriendListRequest(requestBody: String) {
    socialController.handleGetFriendsList(requestBody)
}

fun main(): Unit = runServiceListener(
    mapOf(
        "social:request:ProposeFriendship" to ::proposeFriendshipRequest,
        "social:request:RespondToFriendship" to ::respondToFriendshipRequest,
        "social:request:GetUserProfile" to ::getUserProfileRequest,
        "user_data:response:GetUserData" to ::handleUserDataResponse,
        "social:request:GetFriendsList" to ::getFriendListRequest
    ),
    ::socialServiceStartup
) 