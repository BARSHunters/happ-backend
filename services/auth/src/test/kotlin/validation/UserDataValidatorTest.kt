package validation

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserDataValidatorTest {
    @Test
    fun `valid name passes validation`() {
        assertTrue(UserDataValidator.userDataValidation("Иван Иванов", 180, 75f, LocalDate.of(1990, 1, 1)))
    }

    @Test
    fun `valid short name passes validation`() {
        assertTrue(UserDataValidator.userDataValidation("J. Doe", 180, 75f, LocalDate.of(1990, 1, 1)))
    }

    @Test
    fun `name with numbers fails validation`() {
        assertFalse(UserDataValidator.userDataValidation("Иван123", 180, 75f, LocalDate.of(1990, 1, 1)))
    }

    @Test
    fun `negative height fails validation`() {
        assertFalse(UserDataValidator.userDataValidation("Иван Иванов", -180, 75f, LocalDate.of(1990, 1, 1)))
    }

    @Test
    fun `negative weight fails validation`() {
        assertFalse(UserDataValidator.userDataValidation("Иван Иванов", 180, -75f, LocalDate.of(1990, 1, 1)))
    }

    @Test
    fun `negative birthDate fails validation`() {
        assertFalse(UserDataValidator.userDataValidation("J. Doe", 180, 75f, LocalDate.of(2026, 1, 1)))
    }
}