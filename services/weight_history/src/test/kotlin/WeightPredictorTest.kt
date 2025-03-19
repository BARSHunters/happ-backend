import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WeightPredictorTest {
    private lateinit var weightPredictor: WeightPredictor

    @BeforeEach
    fun setUp() {
        // Создаем объект WeightPredictor
        weightPredictor =
            WeightPredictor(
                gender = "male",
                age = 30,
                height = 176,
                goal = "keep",
            )
    }

    @Test
    fun testPredictNextWeight() {
        // Добавляем записи для прогнозирования
        weightPredictor.addRecord(70.0, 2000.0, 500.0)
        weightPredictor.addRecord(69.5, 2200.0, 600.0)
        weightPredictor.addRecord(69.0, 2100.0, 550.0)

        // Прогнозируем следующий вес
        val predictedWeight = weightPredictor.predictNextWeight()

        // Проверяем, что прогнозируемый вес находится в разумных пределах
        assertTrue(predictedWeight > 0)
    }

    @Test
    fun testCalculateBMR() {
        // Проверяем расчет базового метаболизма для мужчины
        val bmrMale = weightPredictor.calculateBMR(70.0)
        assertEquals(1655.0, bmrMale, 0.1) // Погрешность 0.1

        // Проверяем расчет базового метаболизма для женщины
        weightPredictor =
            WeightPredictor(
                gender = "female",
                age = 30,
                height = 164,
                goal = "keep",
            )
        val bmrFemale = weightPredictor.calculateBMR(60.0)
        assertEquals(1314.0, bmrFemale, 0.1) // Погрешность 0.1
    }

    @Test
    fun testLinearRegressionPrediction() {
        // Добавляем записи для линейной регрессии
        weightPredictor.addRecord(70.0, 2000.0, 500.0)
        weightPredictor.addRecord(69.5, 2200.0, 600.0)
        weightPredictor.addRecord(69.0, 2100.0, 550.0)

        // Прогнозируем вес с использованием линейной регрессии
        val predictedWeight = weightPredictor.linearRegressionPrediction()

        // Проверяем, что прогнозируемый вес находится в разумных пределах
        assertTrue(predictedWeight > 0)
    }
}
