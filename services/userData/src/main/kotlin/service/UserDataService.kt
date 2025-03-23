package service

import model.Gender
import model.UserData
import model.WeightDesire
import repository.UserRepository
import java.time.LocalDate

class UserDataService(private val userRepository: UserRepository) {

    fun createUserData(userData: UserData): Boolean {
        if (userRepository.findByUsername(userData.username) != null) {
            return false
        }
        if (!userRepository.createUser(userData)) {
            return false
        }
        return true
    }

    fun updateUserData(userData: UserData): Boolean {
        val existingUserData = userRepository.findByUsername(userData.username) ?: return false
        val updatedFields = mutableMapOf<String, Any?>()

        if (existingUserData.name != userData.name) {
            updatedFields["name"] = userData.name
        }
        if (existingUserData.birthDate != userData.birthDate) {
            updatedFields["birth_date"] = userData.birthDate
        }
        if (existingUserData.gender != userData.gender) {
            updatedFields["gender"] = userData.gender
        }
        if (existingUserData.heightCm != userData.heightCm) {
            updatedFields["height_cm"] = userData.heightCm
        }
        if (existingUserData.weightKg != userData.weightKg) {
            updatedFields["weight_kg"] = userData.weightKg
        }
        if (existingUserData.weightDesire != userData.weightDesire) {
            updatedFields["weight_desire"] = userData.weightDesire
        }

        if (updatedFields.isNotEmpty()) {
            return userRepository.updateUserFields(userData.username, updatedFields)
        }
        return false
    }

    fun getUserData(username: String): UserData? {
        val userData = userRepository.findByUsername(username) ?: return null
        return userData
    }

    fun getName(username: String): String? {
        return userRepository.findByUsername(username)?.name
    }

    fun getGender(username: String): Gender? {
        return userRepository.findByUsername(username)?.gender
    }

    fun getBirthDate(username: String): LocalDate? {
        return userRepository.findByUsername(username)?.birthDate
    }

    fun getAge(username: String): Int? {
        val birthDate = userRepository.findByUsername(username)?.birthDate ?: return null
        var age = LocalDate.now().minusYears(birthDate.year.toLong()).year
        if (LocalDate.now().dayOfYear > birthDate.dayOfYear) {
            age += 1
        }
        return age
    }

    fun getHeightCm(username: String): Int? {
        return userRepository.findByUsername(username)?.heightCm
    }

    fun getWeightKg(username: String): Float? {
        return userRepository.findByUsername(username)?.weightKg
    }

    fun getWeightDesire(username: String): WeightDesire? {
        return userRepository.findByUsername(username)?.weightDesire
    }
}