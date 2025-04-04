package validation

object AuthValidator {
    fun authValidation(username: String, password: String): Boolean {
        return usernameValidation(username) && passwordValidation(password)
    }

    private fun usernameValidation(username: String): Boolean {
        return username.length >= 5 &&
                username.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
    }

    private fun passwordValidation(password: String): Boolean {
        return password.length >= 8
    }
}