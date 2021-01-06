import kotlin.RuntimeException

class RuntimeError(val token: Token, override val message: String) : RuntimeException()
