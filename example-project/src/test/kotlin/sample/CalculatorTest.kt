package sample

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CalculatorTest {

    private val calculator = Calculator()

    @Test
    fun testAdd() {
        assertEquals(5, calculator.add(2, 3))
    }

    @Test
    fun testSubtract() {
        assertEquals(-1, calculator.subtract(2, 3))
    }

    @Test
    fun testDivide() {
        assertEquals(2, calculator.divide(4, 2))

        var divByZeroCaught = false
        try {
            calculator.divide(1, 0)
        } catch (e: IllegalArgumentException) {
            divByZeroCaught = true
        }
        assertEquals(true, divByZeroCaught, "Division by zero should throw IllegalArgumentException.")
    }

    @Test
    fun testPower() {
        assertEquals(8, calculator.power(2, 3))
        assertEquals(1, calculator.power(5, 0))
        assertEquals(0, calculator.power(0, 5))
        assertEquals(1, calculator.power(0, 0)) // typically 0^0 is considered as 1
    }
}