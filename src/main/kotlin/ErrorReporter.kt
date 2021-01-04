interface ErrorReporterInterface {
    var hadError: Boolean

    fun error(line: Int, message: String)
}

class ErrorReporter : ErrorReporterInterface {
    override var hadError = false

    override fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        println("[line $line] Error $where: $message")
        hadError = true
    }

    fun reset() {
        hadError = false
    }
}
