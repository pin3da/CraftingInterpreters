class AstPrinter {
    var buffer = StringBuilder()

    fun print(expr: Expr): String {
        buffer = StringBuilder()
        traverse(expr)
        return buffer.toString()
    }

    private fun traverse(expr: Expr) {
        when (expr) {
            is Literal -> buffer.append(expr.value.toString())
            is Binary -> parenthesize(expr.op.lexeme, expr.left, expr.right)
            is Unary -> parenthesize(expr.op.lexeme, expr.expr)
            is Grouping -> parenthesize("group", expr.expr)
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
