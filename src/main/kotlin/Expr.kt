sealed class Expr

data class Binary(val left: Expr, val op: Token, val right: Expr) : Expr()
data class Grouping(val expr : Expr) : Expr()
data class Literal(val value: Any?) : Expr()
data class Unary(val op: Token, val expr: Expr) : Expr()