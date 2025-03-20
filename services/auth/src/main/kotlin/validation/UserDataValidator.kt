package validation

object UserDataValidator {
    fun userDataValidation(name: String, heightCm: Int, weightKg: Float): Boolean {
        return nameValidation(name) && heightValidation(heightCm) && weightValidation(weightKg)
    }

    private fun nameValidation(name: String): Boolean {
        return name.length >= 5 &&
                name.matches(Regex("^[a-zA-Zа-яА-ЯёЁ]+\\.?(?: +[a-zA-Zа-яА-ЯёЁ]+\\.?)*$"))
    }

    private fun heightValidation(heightCm: Int): Boolean {
        return heightCm > 0
    }

    private fun weightValidation(weight: Float): Boolean {
        return weight > 0
    }
}