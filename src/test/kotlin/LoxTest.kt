import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class LoxTest {

    @Test
    fun `basic expressions`() {
        val source = "(-1 * 10) * 100 + 12 == 1;"
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)
        val statements = parser.parse()

        assertEquals(1, statements.size)
        assertEquals(
            "(== (+ (* (group (* (- 1.0) 10.0)) 100.0) 12.0) 1.0)",
            AstPrinter().print((statements[0] as Stmt.Expression).expr)
        )
    }

    @Test
    fun `should report parsing errors`() {
        val source = "(-1 * * )"
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)

        assertEquals(0, parser.parse().size)
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
    fun `missing parenthesis should report error`() {
        val source = """(10 * 100"""
        val errorReporter = TestErrorReporter()
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens.toCollection(ArrayDeque()), errorReporter)

        assertEquals(0, parser.parse().size)
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
    fun `interpret arithmetic`() {
        val source = "print (100) + (2 * 4) - (20 / 2);"
        val (_, out) = interpret(source)
        assertEquals(listOf("98"), out.printed)
    }

    @Test
    fun `interpret strings`() {
        val source = """print("it works.");"""
        val (_, out) = interpret(source)
        assertEquals(listOf("it works."), out.printed)
    }

    @Test
    fun `interpret detects invalid types`() {
        val source = """3 < "pancake";"""
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadRuntimeError)
        assertEquals(1, errorReporter.errors.size)
        val err = errorReporter.errors[0] as TestErrorReporter.Error.Interpreter
        assertEquals(Token(TokenType.LESS, "<", null, 1), err.error.token)
        assertEquals("Operands for '<' must be numbers.", err.error.message)
    }

    @Test
    fun `expression must end in semicolon`() {
        val source = "val a = 1"
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadError)
        assertEquals(
            listOf(
                TestErrorReporter.Error.Parser(
                    Token(TokenType.IDENTIFIER, "a", null, 1),
                    "Expect ';' after expression."
                )
            ),
            errorReporter.errors
        )
    }

    @Test
    fun `variable definition`() {
        val source = """
            var a  = 1;
            print(a + 1);
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(listOf("2"), out.printed)
    }


    @Test
    fun `undefined variables should fail`() {
        val source = """
            var a  = 1;
            print(b);
        """.trimIndent()
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadRuntimeError)
        assertEquals(1, errorReporter.errors.size)
        assertEquals(
            "Undefined variable b.",
            (errorReporter.errors[0] as TestErrorReporter.Error.Interpreter).error.message
        )
    }

    @Test
    fun `nested blocks`() {
        val source = """
        var a = "global a";
        var b = "global b";
        var c = "global c";
        {
          var a = "outer a";
          var b = "outer b";
          {
            var a = "inner a";
            print a;
            print b;
            print c;
          }
          print a;
          print b;
          print c;
        }
        print a;
        print b;
        print c;
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(
            listOf(
                "inner a",
                "outer b",
                "global c",
                "outer a",
                "outer b",
                "global c",
                "global a",
                "global b",
                "global c",
            ), out.printed
        )
    }


    private fun interpret(source: String): Pair<TestErrorReporter, Printer> {
        val errorReporter = TestErrorReporter()
        val printer = Printer(false)
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens().toCollection(ArrayDeque())
        val parser = Parser(tokens, errorReporter)
        Interpreter(errorReporter, printer).interpret(parser.parse())
        return Pair(errorReporter, printer)
    }
}