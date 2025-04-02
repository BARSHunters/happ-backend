import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.Gender
import utils.WeightDesire

class WeightPredictorTest {
    private lateinit var weightPredictor: WeightPredictor

    @BeforeEach
    fun setUp() {
        // Создаем объект WeightPredictor
        weightPredictor =
            WeightPredictor(
                gender = Gender.MALE,
                age = 30,
                height = 176,
                goal = WeightDesire.REMAIN,
            )
    }

    @Test
    fun testPredictNextWeight() {
        // Добавляем 1 запись для только эмпирического прогнозирования
        weightPredictor.addRecord(69.0, 2100.0, 550.0)

        // Прогнозируем следующий вес
        val predictedWeight = weightPredictor.predictNextWeight()

        // Проверяем, что прогнозируемый вес находится в разумных пределах
        // Реальный ответ: 68.96038...
        assertEquals(68.960, predictedWeight, 0.001)
    }

    @Test
    fun testCalculateBMR() {
        // Проверяем расчет базового метаболизма для мужчины
        val bmrMale = weightPredictor.calculateBMR(70.0)
        assertEquals(1655.0, bmrMale, 0.1) // Погрешность 0.1

        // Проверяем расчет базового метаболизма для женщины
        weightPredictor =
            WeightPredictor(
                gender = Gender.FEMALE,
                age = 30,
                height = 164,
                goal = WeightDesire.REMAIN,
            )
        val bmrFemale = weightPredictor.calculateBMR(60.0)
        assertEquals(1314.0, bmrFemale, 0.1) // Погрешность 0.1
    }

    @Test
    fun testLinearRegressionPrediction() {
        // Добавляем 3 записи для проверки линейной регрессии
        weightPredictor.addRecord(70.0, 2000.0, 500.0)
        weightPredictor.addRecord(69.5, 2200.0, 600.0)
        weightPredictor.addRecord(69.0, 2100.0, 550.0)

        // Прогнозируем вес с использованием линейной регрессии
        val predictedWeight = weightPredictor.linearRegressionPrediction()

        // Проверяем, что прогнозируемый вес находится в разумных пределах
        // Реальный ответ: 69.4658...
        assertEquals(69.465, predictedWeight, 0.001)
    }
}
