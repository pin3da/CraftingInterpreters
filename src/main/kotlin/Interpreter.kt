class Interpreter(
    private val errorReporter: ErrorReporterInterface,
    private val printer: Printer
) {
    private var environment = Environment()

    fun interpret(statements: List<Stmt>) {
        try {
            for (s in statements) {
                execute(s)
            }
        } catch (error: RuntimeError) {
            errorReporter.error(error)
        }
    }

    private fun execute(stmt: Stmt) {
        when (stmt) {
            is Stmt.Print -> {
                val value = eval((stmt.value))
                printer.print(stringify(value))
            }
            is Stmt.Expression -> eval(stmt.expr)
            is Stmt.Var -> {
                val value = stmt.initializer?.let {
                    eval(it)
                }
                environment.define(stmt.name.lexeme, value)
            }
            is Stmt.Block -> executeBlock(stmt.statements)
            is Stmt.If -> executeIf(stmt)
            is Stmt.While -> executeWhile(stmt)
        }.let { } // Ensure it covers all the branches in the sealed class.
    }

    private fun executeWhile(stmt: Stmt.While) {
        while (isTruthy(eval(stmt.condition))) {
            execute(stmt.body)
        }
    }

    private fun executeIf(stmt: Stmt.If) {
        if (isTruthy(eval(stmt.condition))) {
            execute(stmt.thenBranch)
        } else {
            stmt.elseBranch?.let { execute(it) }
        }
    }

    fun executeInEnv(statements: List<Stmt>, outerEnvironment: Environment) {
        val previous = this.environment
        try {
            this.environment = outerEnvironment
            for (s in statements) {
                execute(s)
            }
        } finally {
            this.environment = previous
        }
    }

    private fun executeBlock(statements: List<Stmt>) {
        executeInEnv(statements, Environment(environment))
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
            is Expr.Literal -> expr.value
            is Expr.Grouping -> eval(expr.expr)
            is Expr.Unary -> evalUnary(expr)
            is Expr.Binary -> evalBinary(expr)
            is Expr.Variable -> environment.get(expr.name)
            is Expr.Assign -> {
                val value = eval(expr.value)
                environment.assign(expr.name, value)
                value
            }
            is Expr.Logical -> evalLogical(expr)
        }
    }

    private fun evalLogical(expr: Expr.Logical): Any? {
        val left = eval(expr.left)
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) {
                return left
            }
        } else {
            if (!isTruthy(left)) {
                return left
            }
        }
        return eval(expr.right)
    }

    private fun evalBinary(expr: Expr.Binary): Any? {
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

    private fun evalUnary(expr: Expr.Unary): Any? {
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
        check(value !is Stmt) { "Must eval Stmt before checking if it is Truthy." }
        if (value is Boolean) {
            return value
        }
        return true
    }
}