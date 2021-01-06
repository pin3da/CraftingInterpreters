class Interpreter(private val errorReporter: ErrorReporterInterface) {

    fun interpret(expr: Expr): String {
        return try {
            stringify(eval(expr))
        } catch (error: RuntimeError) {
            errorReporter.error(error)
            "failed"
        }
    }

    private fun stringify(value: Any?): String {
        if (value == null) return "nil"

        if (value is Double) {
            val text = value.toString()
            if (text.endsWith(".0")) {
                return text.substring(0, text.length - 2)
            }
            return text
        }

        return value.toString()
    }

    private fun eval(expr: Expr): Any? {
        return when (expr) {
            is Literal -> expr.value
            is Grouping -> eval(expr.expr)
            is Unary -> evalUnary(expr)
            is Binary -> evalBinary(expr)
        }
    }

    private fun evalBinary(expr: Binary): Any? {
        fun errorNumbers() {
            throw RuntimeError(expr.op, "Operands for '${expr.op.lexeme}' must be numbers.")
        }

        val left = eval(expr.left)
        val right = eval(expr.right)

        return when (expr.op.type) {
            TokenType.GREATER -> when {
                (left is Double && right is Double) -> left > right
                else -> errorNumbers()
            }
            TokenType.GREATER_EQUAL -> when {
                (left is Double && right is Double) -> left >= right
                else -> errorNumbers()
            }
            TokenType.LESS -> when {
                (left is Double && right is Double) -> left < right
                else -> errorNumbers()
            }
            TokenType.LESS_EQUAL -> when {
                (left is Double && right is Double) -> left <= right
                else -> errorNumbers()
            }
            TokenType.MINUS -> when {
                (left is Double && right is Double) -> left - right
                else -> errorNumbers()
            }
            TokenType.SLASH -> when {
                (left is Double && right is Double) -> left / right
                else -> errorNumbers()
            }
            TokenType.STAR -> when {
                (left is Double && right is Double) -> left * right
                else -> errorNumbers()
            }
            TokenType.PLUS -> {
                when {
                    (left is Double && right is Double) -> left + right
                    (left is String && right is String) -> left + right
                    else -> throw RuntimeError(
                        expr.op,
                        "Operator + is only supported for Number or String."
                    )
                }
            }
            TokenType.BANG_EQUAL -> left != right
            TokenType.EQUAL_EQUAL -> left == right
            else -> throw RuntimeError(expr.op, "Unsupported Binary expression")
        }
    }

    private fun evalUnary(expr: Unary): Any? {
        fun error(msg: String) {
            throw RuntimeError(expr.op, msg)
        }

        val right = eval(expr.expr)
        return when (expr.op.type) {
            TokenType.MINUS -> {
                when (right) {
                    is Double -> -right
                    else -> error("Must be Number.")
                }
            }
            TokenType.BANG -> !isTruthy(right)
            else -> error("Unsupported unary expression ${expr.op.type}")
        }
    }

    private fun isTruthy(value: Any?): Boolean {
        if (value == null) {
            return false
        }
        if (value is Boolean) {
            return value
        }
        return true
    }
}
