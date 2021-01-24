class Interpreter(
    private val errorReporter: ErrorReporterInterface,
    private val printer: Printer
) {
    private val globals = Environment()
    private var environment = globals
    private val locals = mutableMapOf<Expr, Int>()

    init {
        globals.define(
            "clock",
            object : LoxCallable {
                override fun arity(): Int {
                    return 0
                }

                override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
                    return System.currentTimeMillis().toDouble() / 1000.0
                }

                override fun toString(): String {
                    return "<native fn>"
                }
            }
        )
    }

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
            is Stmt.Var -> executeVar(stmt)
            is Stmt.Block -> executeBlock(stmt.statements)
            is Stmt.If -> executeIf(stmt)
            is Stmt.While -> executeWhile(stmt)
            is Stmt.Function -> executeFunction(stmt)
            is Stmt.Return -> executeReturn(stmt)
            is Stmt.Class -> executeClass(stmt)
        }.let { } // Ensure it covers all the branches in the sealed class.
    }

    private fun eval(expr: Expr): Any? {
        return when (expr) {
            is Expr.Literal -> expr.value
            is Expr.Grouping -> eval(expr.expr)
            is Expr.Unary -> evalUnary(expr)
            is Expr.Binary -> evalBinary(expr)
            is Expr.Variable -> lookUpVariable(expr.name, expr)
            is Expr.Assign -> evalAssign(expr)
            is Expr.Logical -> evalLogical(expr)
            is Expr.Call -> evalCall(expr)
            is Expr.Get -> evalGet(expr)
            is Expr.Set -> evalSet(expr)
        }
    }

    private fun evalSet(expr: Expr.Set): Any? {
        val obj = eval(expr.obj)

        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }

        val value = eval(expr.value)
        obj.set(expr.name, value)
        return value
    }

    private fun evalGet(expr: Expr.Get): Any? {
        val obj = eval(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }
        throw RuntimeError(expr.name, "Only instances have properties.")
    }

    private fun executeClass(stmt: Stmt.Class) {
        environment.define(stmt.name.lexeme, null)
        val klass = LoxClass(stmt.name.lexeme)
        environment.assign(stmt.name, klass)
    }

    private fun executeReturn(stmt: Stmt.Return): Any? {
        val value = stmt.value?.let { eval(it) }
        throw Return(value)
    }

    private fun executeVar(stmt: Stmt.Var) {
        val value = stmt.initializer?.let { eval(it) }
        environment.define(stmt.name.lexeme, value)
    }

    private fun executeFunction(stmt: Stmt.Function): Any? {
        val function = LoxFunction(stmt, environment)
        environment.define(stmt.name.lexeme, function)
        return null
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

    private fun evalAssign(expr: Expr.Assign): Any? {
        val value = eval(expr.value)
        val dist = locals[expr]
        if (dist != null) {
            environment.assignAt(dist!!, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val dist = locals[expr] ?: return globals.get(name)
        return environment.getAt(dist, name.lexeme)
    }

    private fun evalCall(expr: Expr.Call): Any? {
        val callee = eval(expr.callee)
        val arguments = expr.arguments.map { eval(it) }
        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes.")
        }
        val function: LoxCallable = callee
        if (arguments.size != function.arity()) {
            throw RuntimeError(
                expr.paren,
                "Expected ${function.arity()} arguments but got ${arguments.size}."
            )
        }
        return function.call(this, arguments)
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

    fun resolve(expr: Expr, steps: Int) {
        locals[expr] = steps
    }
}
