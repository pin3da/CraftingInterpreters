sealed class Stmt {
    data class Print(val value: Expr) : Stmt()
    data class Expression(val expr: Expr) : Stmt()
    data class Var(val name: Token, val initializer: Expr?) : Stmt()
    data class Block(val statements: List<Stmt>) : Stmt()
    data class If(val contidtion: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
}
