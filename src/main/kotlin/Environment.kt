class Environment(private val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    @Throws(RuntimeError::class)
    fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        if (enclosing != null) {
            return enclosing.get(name)
        }
        throw RuntimeError(name, "Undefined variable ${name.lexeme}.")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw RuntimeError(name, "Undefined variable ${name.lexeme}.")
    }

    fun getAt(dist: Int, name: String): Any? {
        return ancestor(dist).values[name]
    }

    fun assignAt(dist: Int, name: Token, value: Any?) {
        ancestor(dist).values[name.lexeme] = value
    }

    private fun ancestor(dist: Int): Environment {
        var env = this
        for (i in 0 until dist) {
            env = env.enclosing!!
        }
        return env
    }
}
