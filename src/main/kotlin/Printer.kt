class Printer(private val printOut : Boolean) {
    val printed = mutableListOf<String>()

    fun print(s: String) {
        printed.add(s)
        if (printOut) {
            println(s)
        }
    }
}