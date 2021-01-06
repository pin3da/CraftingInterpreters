interface ErrorReporterInterface {
    var hadError: Boolean
    var hadRuntimeError: Boolean

    fun error(line: Int, message: String)
    fun error(token: Token, message: String)
    fun error(error: RuntimeError)
}

class ErrorReporter : ErrorReporterInterface {
    override var hadError = false
    override var hadRuntimeError = false

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

    override fun error(error: RuntimeError) {
        println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }

    private fun report(line: Int, where: String, message: String) {
        println("[line $line] Error $where: $message")
        hadError = true
    }

    fun reset() {
        hadError = false
    }
}

class TestErrorReporter : ErrorReporterInterface {
    sealed class Error {
        data class Lexer(val line: Int, val message: String) : Error()
        data class Parser(val token: Token, val message: String) : Error()
        data class Interpreter(val error: RuntimeError) : Error()
    }

    override var hadError = false
    override var hadRuntimeError = false

    val errors = mutableListOf<Error>()

    override fun error(line: Int, message: String) {
        errors.add(Error.Lexer(line, message))
        hadError = true
    }

    override fun error(token: Token, message: String) {
        errors.add(Error.Parser(token, message))
        hadError = true
    }

    override fun error(error: RuntimeError) {
        errors.add(Error.Interpreter(error))
        hadRuntimeError = true
    }
}
