package org.example.calculators

/**
 * Абстрактный калькулятор
 *
 * В своей сути паттерн команда
 */
interface Calculator {
    fun calculate(): Result<Double>
}