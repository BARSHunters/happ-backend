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

fun createUserData(requestBody: String) {
    userDataController.handleCreateUserData(requestBody)
}

fun updateUserData(requestBody: String) {
    userDataController.handleUpdateUserData(requestBody)
}

fun getUserData(requestBody: String) {
    userDataController.receiveUserData(requestBody)
}

fun getName(requestBody: String) {
    userDataController.receiveName(requestBody)
}

fun getGender(requestBody: String) {
    userDataController.receiveGender(requestBody)
}

fun getBirthDate(requestBody: String) {
    userDataController.receiveBirthDate(requestBody)
}

fun getAge(requestBody: String) {
    userDataController.receiveAge(requestBody)
}

fun getHeight(requestBody: String) {
    userDataController.receiveHeight(requestBody)
}

fun getWeight(requestBody: String) {
    userDataController.receiveWeight(requestBody)
}

fun getWeightDesire(requestBody: String) {
    userDataController.receiveWeightDesire(requestBody)
}

fun main(): Unit = runServiceListener(
    mapOf(
        "user_data:request:CreateUserData" to ::createUserData,
        "user_data:request:UpdateUserData" to ::updateUserData,
        "user_data:request:UserData" to ::getUserData,
        "user_data:request:Name" to ::getName,
        "user_data:request:Gender" to ::getGender,
        "user_data:request:BirthDate" to ::getBirthDate,
        "user_data:request:Age" to ::getAge,
        "user_data:request:Height" to ::getHeight,
        "user_data:request:Weight" to ::getWeight,
        "user_data:request:WeightDesire" to ::getWeightDesire,
    ),
    ::afterStartup
)