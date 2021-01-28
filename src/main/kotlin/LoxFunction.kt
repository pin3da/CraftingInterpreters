class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) :
    LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for ((i, param) in declaration.params.withIndex()) {
            environment.define(param.lexeme, arguments[i])
        }
        try {
            interpreter.executeInEnv(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) {
                return closure.getAt(0, "this")
            }
            return returnValue.value
        }
        if (isInitializer) {
            return closure.getAt(0, "this")
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

    fun bind(loxInstance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", loxInstance)
        return LoxFunction(declaration, environment, isInitializer)
    }
}
