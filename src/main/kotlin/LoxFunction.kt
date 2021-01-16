class LoxFunction(private val declaration: Stmt.Function) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>) : Any? {
        val environment = Environment(interpreter.globals)
        for ((i, param) in declaration.params.withIndex()) {
            environment.define(param.lexeme, arguments[i])
        }
        try {
            interpreter.executeInEnv(declaration.body, environment)
        } catch (returnValue : Return) {
            return returnValue.value
        }
        return null
    }

    override fun arity(): Int {
        return declaration.params.size
    }

    override fun toString(): String {
        val params = declaration.params.map { it.lexeme }
        return "<fn ${declaration.name.lexeme} ($params)>"
    }
}