import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

object Lox {
    private val errorReporter = ErrorReporter()
    private val interpreter = Interpreter(errorReporter, Printer(true))

    @JvmStatic
    fun main(args: Array<String>) {
        when (args.size) {
            0 -> runPrompt()
            1 -> runFile(args[0])
            else -> {
                println("Usage jlox [script]")
                exitProcess(64)
            }
        }
    }

    private fun runFile(path: String) {
        runLox(File(path).readText())
        if (errorReporter.hadError) exitProcess(65)
        if (errorReporter.hadRuntimeError) exitProcess(70)
    }

    private fun runPrompt() {
        val input = InputStreamReader(System.`in`)
        val reader = BufferedReader(input)

        while (true) {
            print("> ")
            val line = reader.readLine() ?: break
            runLox(line)
            errorReporter.reset()
        }
    }

    private fun runLox(source: String) {
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)
        val statements = parser.parse()

        if (errorReporter.hadError) {
            return
        }
        interpreter.interpret(statements)
    }
}
