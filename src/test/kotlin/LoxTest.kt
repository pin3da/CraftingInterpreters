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
            ),
            out.printed
        )
    }

    @Test
    fun `eval logical operators`() {
        val source = """
            print nil or "manu";
            print "manu" and "chao";
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(listOf("manu", "chao"), out.printed)
    }

    @Test
    fun `eval loops`() {
        val source = """
          var a = 0;
          var temp;

          for (var b = 1; a < 100; b = temp + b) {
            print a;
            temp = a;
            a = b;
          }
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(
            listOf("0", "1", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89"),
            out.printed
        )
    }

    @Test
    fun `eval recursion and return`() {
        val source = """
            fun fib(n) {
              if (n <= 1) {
                return n;
              }
              return fib(n - 2) + fib(n - 1);
            }

            for (var i = 0; i < 12; i = i + 1) {
              print fib(i);
            }
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(
            listOf("0", "1", "1", "2", "3", "5", "8", "13", "21", "34", "55", "89"),
            out.printed
        )
    }

    @Test
    fun `supports closures`() {
        val source = """
            fun makeCounter(name) {
               var i = 0;
               fun count() {
                  i = i + 1;
                  print "from " + name;
                  print i;
               }
               return count;
            }
            var counter1 = makeCounter("First");
            var counter2 = makeCounter("Second");
            counter1();
            counter2();
            counter1();
            counter2();
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(
            listOf(
                "from First", "1",
                "from Second", "1",
                "from First", "2",
                "from Second", "2"
            ),
            out.printed
        )
    }

    @Test
    fun `should not read variable in its own initializer`() {
        val source = """
            var a = "outer";
            {
              var a = a;
            }
        """.trimIndent()
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadError)
        assertEquals(1, errorReporter.errors.size)
        assertEquals(
            "Can't read local variable in its own initializer.",
            (errorReporter.errors[0] as TestErrorReporter.Error.Parser).message
        )
    }

    @Test
    fun `should not redeclare variable in the same scope`() {
        val source = """
            fun bad() {
              var a = "first";
              var a = "second";
            }
        """.trimIndent()
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadError)
        assertEquals(1, errorReporter.errors.size)
        assertEquals(
            "Variable already declared in this scope.",
            (errorReporter.errors[0] as TestErrorReporter.Error.Parser).message
        )
    }

    @Test
    fun `should not return at top-level scope`() {
        val source = """
            return "at top level";
        """.trimIndent()
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadError)
        assertEquals(1, errorReporter.errors.size)
        assertEquals(
            "Can not return from top-level code.",
            (errorReporter.errors[0] as TestErrorReporter.Error.Parser).message
        )
    }

    @Test
    fun `supports classes and 'this'`() {
        val source = """
            class Thing {
              getCallback() {
                fun localFunction() {
                  print this;
                  print "from local function";
                }
            
                return localFunction;
              }
            }
            
            var callback = Thing().getCallback();
            callback();
        """.trimIndent()
        val (_, out) = interpret(source)
        assertEquals(
            listOf(
                "Thing instance",
                "from local function"
            ),
            out.printed
        )
    }

    @Test
    fun `should fail to resolve 'this' outside methods`() {
        val source = """
        fun notAMethod() {
          print this;
        }
        """.trimIndent()
        val (errorReporter, _) = interpret(source)
        assertTrue(errorReporter.hadError)
        assertEquals(1, errorReporter.errors.size)
        assertEquals(
            "Can't use 'this' outside of a class",
            (errorReporter.errors[0] as TestErrorReporter.Error.Parser).message
        )
    }

    private fun interpret(source: String): Pair<TestErrorReporter, Printer> {
        val errorReporter = TestErrorReporter()
        val printer = Printer(false)
        val scanner = Scanner(source, errorReporter)
        val tokens = scanner.scanTokens().toCollection(ArrayDeque())
        val parser = Parser(tokens, errorReporter)
        val interpreter = Interpreter(errorReporter, printer)
        val statements = parser.parse()
        val resolver = Resolver(interpreter, errorReporter)
        resolver.resolve(statements)
        if (errorReporter.hadError) {
            return Pair(errorReporter, printer)
        }
        interpreter.interpret(statements)
        return Pair(errorReporter, printer)
    }
}
