import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.BeforeEach
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

        assertEquals("(== (+ (* (group (* (- 1.0) 10.0)) 100.0) 12.0) 1.0)", AstPrinter().print(expr))
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
}