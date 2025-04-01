package validation

import java.time.LocalDate

object UserDataValidator {
    fun userDataValidation(
        name: String, heightCm: Int,
        weightKg: Float, birthDate: LocalDate
    ): Boolean {
        return nameValidation(name) && heightValidation(heightCm) &&
                weightValidation(weightKg) && birthDateValidation(birthDate)
    }

    private fun nameValidation(name: String): Boolean {
        return name.isNotEmpty() &&
                name.matches(Regex("^[a-zA-Zа-яА-ЯёЁ]+\\.?(?: +[a-zA-Zа-яА-ЯёЁ]+\\.?)*$"))
    }

    private fun birthDateValidation(birthDate: LocalDate): Boolean {
        return birthDate.isBefore(LocalDate.now())
    }

    private fun heightValidation(heightCm: Int): Boolean {
        return heightCm > 0
    }

    private fun weightValidation(weight: Float): Boolean {
        return weight > 0
    }
}