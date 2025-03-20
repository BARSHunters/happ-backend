package validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthValidatorTest {
    @Test
    fun `valid username passes validation`() {
        assertTrue(AuthValidator.authValidation("validUser", "StrongPass123"))
    }

    @Test
    fun `username with special characters fails validation`() {
        assertFalse(AuthValidator.authValidation("invalid@user", "StrongPass123"))
    }

    @Test
    fun `password shorter than 8 characters fails validation`() {
        assertFalse(AuthValidator.authValidation("validUser", "short"))
    }
}