import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AstPrinterTest {
    @Test
    fun print() {
        val printer = AstPrinter()
        val expr = Expr.Binary(
            Expr.Unary(
                Token(TokenType.MINUS, "-", null, 1),
                Expr.Literal(123)
            ),
            Token(TokenType.STAR, "*", null, 1),
            Expr.Grouping(
                Expr.Literal(45.67)
            )
        )
        assertEquals("(* (- 123) (group 45.67))", printer.print(expr))
    }
}
