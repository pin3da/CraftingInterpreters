import java.util.Stack

class Resolver(
    private val interpreter: Interpreter,
    private val errorReporter: ErrorReporterInterface,
) {
    enum class FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD
    }

    enum class ClassType {
        NONE,
        CLASS,
        SUBCLASS
    }

    private val scopes = Stack<MutableMap<String, Boolean>>()
    private var currentFunctionType = FunctionType.NONE
    private var currentClassType = ClassType.NONE

    fun resolve(statements: List<Stmt>) {
        for (statement in statements) {
            resolve(statement)
        }
    }

    // Downcast to the proper resolver.
    private fun resolve(stmt: Stmt) {
        when (stmt) {
            is Stmt.Block -> resolve(stmt)
            is Stmt.Class -> resolve(stmt)
            is Stmt.Function -> resolve(stmt)
            is Stmt.Expression -> resolve(stmt.expr)
            is Stmt.Print -> resolve(stmt.value)
            is Stmt.Return -> resolve(stmt)
            is Stmt.While -> resolve(stmt)
            is Stmt.Var -> resolve(stmt)
            is Stmt.If -> resolve(stmt)
        }.let { } // Catch missing branches.
    }

    private fun resolve(expr: Expr) {
        when (expr) {
            is Expr.Variable -> resolve(expr)
            is Expr.Assign -> resolve(expr)
            is Expr.Binary -> resolve(expr)
            is Expr.Call -> resolve(expr)
            is Expr.Get -> resolve(expr)
            is Expr.Grouping -> resolve(expr.expr)
            is Expr.Literal -> {
            } // not binded to anything.
            is Expr.Logical -> resolve(expr)
            is Expr.Set -> resolve(expr)
            is Expr.Super -> resolve(expr)
            is Expr.Unary -> resolve(expr.expr)
            is Expr.This -> resolve(expr)
        }.let { }
    }

    private fun resolve(stmt: Stmt.Class) {
        val enclosingClassType = currentClassType
        currentClassType = ClassType.CLASS
        declare(stmt.name)
        define(stmt.name)

        if (stmt.superClass != null && stmt.name.lexeme == stmt.superClass.name.lexeme) {
            errorReporter.error(stmt.superClass.name, "A class can't inherit from itself.")
        }

        stmt.superClass?.let {
            currentClassType = ClassType.SUBCLASS
            resolve(it)
        }

        stmt.superClass?.let {
            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()
        scopes.peek()["this"] = true

        for (method in stmt.methods) {
            val declaration =
                if (method.name.lexeme == "init") FunctionType.INITIALIZER else FunctionType.METHOD
            resolveFunction(method, declaration)
        }

        endScope()
        stmt.superClass?.let { endScope() }

        currentClassType = enclosingClassType
    }

    private fun resolve(stmt: Stmt.Return) {
        if (currentFunctionType == FunctionType.NONE) {
            errorReporter.error(stmt.keyword, "Can not return from top-level code.")
        }
        stmt.value?.let {
            if (currentFunctionType == FunctionType.INITIALIZER) {
                errorReporter.error(
                    stmt.keyword,
                    "Can't return a value from an initializer"
                )
            }
            resolve(it)
        }
    }

    private fun resolve(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunctionType = currentFunctionType
        currentFunctionType = type

        beginScope()
        for (param in function.params) {
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunctionType = enclosingFunctionType
    }

    private fun resolve(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    private fun resolve(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    private fun resolve(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    private fun resolve(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    private fun resolve(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    private fun resolve(expr: Expr.Call) {
        resolve(expr.callee)
        for (argument in expr.arguments) {
            resolve(argument)
        }
    }

    private fun resolve(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    private fun resolve(expr: Expr.Variable) {
        if (!scopes.isEmpty() && scopes.peek()[expr.name.lexeme] == false) {
            errorReporter.error(
                expr.name,
                "Can't read local variable in its own initializer."
            )
        }
        resolveLocal(expr, expr.name)
    }

    private fun resolve(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.indices.reversed()) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolve(expr: Expr.Get) {
        resolve(expr.obj)
    }

    private fun resolve(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    private fun resolve(expr: Expr.This) {
        if (currentClassType == ClassType.NONE) {
            errorReporter.error(expr.keyword, "Can't use 'this' outside of a class")
            return
        }
        resolveLocal(expr, expr.keyword)
    }

    private fun resolve(expr: Expr.Super) {
        when (currentClassType) {
            ClassType.SUBCLASS -> resolveLocal(expr, expr.keyword)
            ClassType.NONE -> errorReporter.error(
                expr.keyword,
                "Can't use 'super' outside of a class."
            )
            else -> errorReporter.error(
                expr.keyword,
                "Can't use 'super' without a superclass."
            )
        }
    }

    private fun define(name: Token) {
        if (scopes.isEmpty()) {
            return
        }
        scopes.peek()[name.lexeme] = true
    }

    private fun declare(name: Token) {
        if (scopes.isEmpty()) {
            return
        }
        val scope = scopes.peek()
        if (scope.contains(name.lexeme)) {
            errorReporter.error(
                name,
                "Variable already declared in this scope."
            )
            return
        }
        scope[name.lexeme] = false
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun beginScope() {
        scopes.push(mutableMapOf())
    }
}
