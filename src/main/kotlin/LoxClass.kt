class LoxClass(val name: String, val methods: Map<String, LoxFunction>) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any {
        return LoxInstance(this)
    }

    override fun arity(): Int {
        return 0
    }

    override fun toString(): String {
        return name
    }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        }
        return null
    }
}
