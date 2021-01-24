class LoxClass(val name: String) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        return LoxInstance(this)
    }

    override fun arity(): Int {
        return 0
    }

    override fun toString(): String {
        return name
    }
}
