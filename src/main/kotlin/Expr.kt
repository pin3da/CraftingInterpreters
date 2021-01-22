sealed class Expr {
    class Assign(val name: Token, val value: Expr) : Expr()
    class Binary(val left: Expr, val op: Token, val right: Expr) : Expr()
    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr()
    class Grouping(val expr: Expr) : Expr()
    class Literal(val value: Any?) : Expr()
    class Logical(val left: Expr, val operator: Token, val right: Expr) : Expr()
    class Unary(val op: Token, val expr: Expr) : Expr()
    class Variable(val name: Token) : Expr()
}
