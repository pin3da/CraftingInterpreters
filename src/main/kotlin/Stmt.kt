sealed class Stmt {
    class Print(val value: Expr) : Stmt()
    class Return(val keyword: Token, val value: Expr?) : Stmt()
    class Expression(val expr: Expr) : Stmt()
    class Function(val name: Token, val params: List<Token>, val body: List<Stmt>) : Stmt()
    class Var(val name: Token, val initializer: Expr?) : Stmt()
    class Block(val statements: List<Stmt>) : Stmt()
    class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt()
    class While(val condition: Expr, val body: Stmt) : Stmt()
}
