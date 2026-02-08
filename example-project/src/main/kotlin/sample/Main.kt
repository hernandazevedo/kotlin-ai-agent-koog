package sample

fun main(args: Array<String>) {
    val calculator = Calculator()

    println("Adding 5 + 3: ${calculator.add(5, 3)}")
    println("Subtracting 5 - 3: ${calculator.subtract(5, 3)}")
    try {
        println("Dividing 5 / 0: ${calculator.divide(5, 0)}")
    } catch (e: IllegalArgumentException) {
        println(e.message)
    }
}