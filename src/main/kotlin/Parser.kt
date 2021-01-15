class Parser(
    private val tokens: ArrayDeque<Token>,
    private val errorReporter: ErrorReporterInterface
) {
    private class ParseError : RuntimeException()

    private var current = 0

    fun parse(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        return statements
    }

    private fun declaration(): Stmt? {
        return try {
            if (match(TokenType.VAR)) {
                varDeclaration()
            } else {
                statement()
            }
        } catch (error: ParseError) {
            synchronize()
            null
        }
    }

    private fun varDeclaration(): Stmt {
        val name = consume(TokenType.IDENTIFIER, "Expect variable name")

        val initializer = if (match(TokenType.EQUAL)) expression() else null

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt {
        return when {
            match(TokenType.FOR) -> forStatement()
            match(TokenType.IF) -> ifStatement()
            match(TokenType.PRINT) -> printStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }
    }

    private fun forStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after for.")
        val initializer = when {
            match(TokenType.SEMICOLON) -> null
            match(TokenType.VAR) -> varDeclaration()
            else -> expressionStatement()
        }
        val condition = if (!check(TokenType.SEMICOLON)) expression() else Expr.Literal(true)
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.")
        val increment = if (!check(TokenType.RIGHT_PAREN)) expression() else null
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")
        var body = statement()
        increment?.let { body = Stmt.Block(listOf(body, Stmt.Expression(it))) }
        body = Stmt.While(condition, body)
        initializer?.let { body = Stmt.Block(listOf(it, body)) }
        return body
    }

    private fun whileStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after while.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.")
        val body = statement()
        return Stmt.While(condition, body)
    }

    private fun block(): List<Stmt> {
        val statements = mutableListOf<Stmt>()
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        return statements
    }

    private fun ifStatement(): Stmt {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")

        var thenBranch = statement()
        var elseBranch = if (match(TokenType.ELSE)) statement() else null
        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun expression(): Expr {
        return assignment()
    }

    private fun assignment(): Expr {
        var expr = or()
        if (match(TokenType.EQUAL)) {
            val equals = previous()
            val value = assignment()

            if (expr is Expr.Variable) {
                val name = expr.name
                return Expr.Assign(name, value)
            }
            error(equals, "Invalid assigment target.")
        }

        return expr
    }

    private fun or(): Expr {
        var expr = and()
        while (match(TokenType.OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun equality(): Expr {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun previous(): Token {
        return tokens[current - 1]
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun advance(): Token {
        if (!isAtEnd()) {
            current++
        }
        return previous()
    }

    private fun synchronize() {
        advance()

        while (!isAtEnd()) {
            if (previous().type === TokenType.SEMICOLON) {
                return
            }
            when (peek().type) {
                TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF,
                TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> {
                    return
                }
            }
            advance()
        }
    }

    private fun comparison(): Expr {
        var expr = term()

        while (match(
                TokenType.GREATER,
                TokenType.GREATER_EQUAL,
                TokenType.LESS,
                TokenType.LESS_EQUAL
            )
        ) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous()
            val right: Expr = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        return when {
            match(TokenType.FALSE) -> Expr.Literal(false)
            match(TokenType.TRUE) -> Expr.Literal(true)
            match(TokenType.NIL) -> Expr.Literal(null)
            match(TokenType.NUMBER, TokenType.STRING) -> Expr.Literal(previous().literal)
            match(TokenType.IDENTIFIER) -> Expr.Variable(previous())
            match(TokenType.LEFT_PAREN) -> {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                Expr.Grouping(expr)
            }
            else -> throw error(peek(), "Expect expression.")
        }
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) {
            return advance()
        }
        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        errorReporter.error(token, message)
        return ParseError()
    }

    private fun check(type: TokenType): Boolean {
        if (isAtEnd()) {
            return false
        }
        return peek().type === type
    }

    private fun peek(): Token {
        return tokens[current]
    }

    private fun isAtEnd(): Boolean {
        return peek().type == TokenType.EOF
    }
}
