import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AstPrinterTest {
    @Test
    fun print() {
        val printer = AstPrinter()
        val expr = Binary(
            Unary(
                Token(TokenType.MINUS, "-", null, 1),
                Literal(123)
            ),
            Token(TokenType.STAR, "*", null, 1),
            Grouping(
                Literal(45.67)
            )
        )
        assertEquals("(* (- 123) (group 45.67))", printer.print(expr))
    }
}
