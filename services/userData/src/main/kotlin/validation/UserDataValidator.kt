package validation

import model.UserData
import java.time.LocalDate

object UserDataValidator {
    fun userDataValidation(userData: UserData): Boolean {
        return usernameValidation(userData.username) && nameValidation(userData.name) &&
                birthDateValidation(userData.birthDate) && heightValidation(userData.heightCm) &&
                weightValidation(userData.weightKg)
    }

    private fun usernameValidation(username: String): Boolean {
        return username.length >= 5 &&
                username.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
    }

    private fun nameValidation(name: String): Boolean {
        return name.isNotEmpty() &&
                name.matches(Regex("^[a-zA-Zа-яА-ЯёЁ]+\\.?(?: +[a-zA-Zа-яА-ЯёЁ]+\\.?)*$"))
    }

    private fun birthDateValidation(birthDate: LocalDate): Boolean {
        return birthDate.isBefore(LocalDate.now())
    }

    private fun heightValidation(height: Int): Boolean {
        return height > 0
    }

    private fun weightValidation(weight: Float): Boolean {
        return weight > 0
    }
}