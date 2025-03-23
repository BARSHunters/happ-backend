import controller.UserDataController
import database.Database
import keydb.runServiceListener
import repository.UserRepository
import service.UserDataService

lateinit var userDataController: UserDataController
lateinit var userDataService: UserDataService

fun afterStartup() {
    Database
    val userRepository = UserRepository()
    userDataService = UserDataService(userRepository)
    userDataController = UserDataController(userDataService)
    println("Service userData is running")
}

fun createUserData(userData: String) {
    userDataController.handleCreateUserData(userData)
}

fun updateUserData(userData: String) {
    userDataController.handleUpdateUserData(userData)
}

fun getUserData(username: String) {
    userDataController.receiveUserData(username)
}

fun getName(username: String) {
    userDataController.receiveName(username)
}

fun getGender(username: String) {
    userDataController.receiveGender(username)
}

fun getBirthDate(username: String) {
    userDataController.receiveBirthDate(username)
}

fun getAge(username: String) {
    userDataController.receiveAge(username)
}

fun getHeight(username: String) {
    userDataController.receiveHeight(username)
}

fun getWeight(username: String) {
    userDataController.receiveWeight(username)
}

fun getWeightDesire(username: String) {
    userDataController.receiveWeightDesire(username)
}

fun main(): Unit = runServiceListener(
    mapOf(
        "createUserDataRequest" to ::createUserData,
        "updateUserData" to ::updateUserData,
        "getUserData" to ::getUserData,
        "getName" to ::getName,
        "getGender" to ::getGender,
        "getBirthDate" to ::getBirthDate,
        "getAge" to ::getAge,
        "getHeight" to ::getHeight,
        "getWeight" to ::getWeight,
        "getWeightDesire" to ::getWeightDesire,
    ),
    ::afterStartup
)