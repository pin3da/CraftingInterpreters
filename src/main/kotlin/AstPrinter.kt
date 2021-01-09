class AstPrinter {
    var buffer = StringBuilder()

    fun print(expr: Expr): String {
        buffer = StringBuilder()
        traverse(expr)
        return buffer.toString()
    }

    private fun traverse(expr: Expr) {
        when (expr) {
            is Expr.Literal -> buffer.append(expr.value.toString())
            is Expr.Binary -> parenthesize(expr.op.lexeme, expr.left, expr.right)
            is Expr.Unary -> parenthesize(expr.op.lexeme, expr.expr)
            is Expr.Grouping -> parenthesize("group", expr.expr)
            else -> error("no print implemented.")
        }
    }

    private fun parenthesize(name: String, vararg exprs: Expr) {
        buffer.append("(").append(name)
        for (expr in exprs) {
            buffer.append(" ")
            traverse(expr)
        }
        buffer.append(")")
    }
}
