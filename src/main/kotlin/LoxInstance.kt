class LoxInstance(private val loxClass: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    override fun toString(): String {
        return loxClass.name + " instance"
    }

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }
        throw RuntimeError(name, "Undefined property '$name'.")
    }

    fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
