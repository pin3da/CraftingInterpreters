interface ErrorReporterInterface {
    var hadError: Boolean

    fun error(line: Int, message: String)
    fun error(token: Token, message: String) {
        error("override if needed")
    }
}

class ErrorReporter : ErrorReporterInterface {
    override var hadError = false

    override fun error(line: Int, message: String) {
        report(line, "", message)
    }

    override fun error(token: Token, message: String) {
        val where = when (token.type) {
            TokenType.EOF -> "at end"
            else -> "at '${token.lexeme}'"
        }
        report(token.line, where, message)
    }

    private fun report(line: Int, where: String, message: String) {
        println("[line $line] Error $where: $message")
        hadError = true
    }

    fun reset() {
        hadError = false
    }
}
