import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LoxTest {

    @Test
    fun basicExpressions() {
        val source = "(-1 * 10) * 100 + 12 == 1"
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)
        val expr = parser.parse()!!

        assertEquals(
            "(== (+ (* (group (* (- 1.0) 10.0)) 100.0) 12.0) 1.0)",
            AstPrinter().print(expr)
        )
    }

    @Test
    fun reportsParsingErrors() {
        val source = "(-1 * * )"
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)

        assertNull(parser.parse())
        assertTrue(errorReporter.hadError)
        assertEquals(
            listOf(
                TestErrorReporter.Error.Parser(
                    Token(TokenType.STAR, "*", null, 1),
                    "Expect expression."
                )
            ),
            errorReporter.errors
        )
    }

    @Test
    fun missingParenthesis() {
        val source = """(10 * 100"""
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)

        assertNull(parser.parse())
        assertTrue(errorReporter.hadError)
        assertEquals(
            listOf(
                TestErrorReporter.Error.Parser(
                    Token(TokenType.EOF, "", null, 1),
                    "Expect ')' after expression."
                )
            ),
            errorReporter.errors
        )
    }

    @Test
    fun interpretArithmetic() {
        val (value, _) = interpret("(100) + (2 * 4) - (20 / 2)")
        assertEquals("98", value)
    }

    @Test
    fun interpretStrings() {
        val (value, _) = interpret("\"it \" + \"works.\"")
        assertEquals("it works.", value)
    }

    @Test
    fun `interpret detects invalid types`() {
        val (value, errorReporter) = interpret("3 < \"pancake\"")
        assertTrue(errorReporter.hadRuntimeError)
        assertEquals("failed", value)
        assertEquals(1, errorReporter.errors.size)
        val err = errorReporter.errors[0] as TestErrorReporter.Error.Interpreter
        assertEquals(Token(TokenType.LESS, "<", null, 1), err.error.token)
        assertEquals("Operands for '<' must be numbers.", err.error.message)
    }

    private fun interpret(source: String): Pair<String, TestErrorReporter> {
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)
        val expr = parser.parse()!!
        return Pair(Interpreter(errorReporter).interpret(expr), errorReporter)
    }
}
