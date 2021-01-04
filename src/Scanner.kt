import TokenType.*

class Scanner(private val source: String, private val errorReporter: ErrorReporterInterface) {
  private val tokens = mutableListOf<Token>()
  var start = 0
  var current = 0
  var line = 1
  companion object {
    val keywords = mapOf(
      "and" to AND,
      "class" to CLASS,
      "else" to ELSE,
      "false" to FALSE,
      "for" to FOR,
      "fun" to FUN,
      "if" to IF,
      "nil" to NIL,
      "or" to OR,
      "print" to PRINT,
      "return" to RETURN,
      "super" to SUPER,
      "this" to THIS,
      "true" to TRUE,
      "var" to VAR,
      "while" to WHILE,
    )
  }

  private fun isAtEnd() : Boolean {
    return current >= source.length
  }

  fun scanTokens(): List<Token> {
    while (!isAtEnd()) {
      start = current
      scanToken()
    }
    tokens.add(Token(EOF, "", null, line))
    return tokens
  }

  private fun scanToken() {
    when (val c = advance()) {
      '(' -> addToken(LEFT_PAREN)
      ')' -> addToken(RIGHT_PAREN)
      '{' -> addToken(LEFT_BRACE)
      '}' -> addToken(RIGHT_BRACE)
      ',' -> addToken(COMMA)
      '.' -> addToken(DOT)
      '-' -> addToken(MINUS)
      '+' -> addToken(PLUS)
      ';' -> addToken(SEMICOLON)
      '*' -> addToken(STAR)
      '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
      '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
      '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
      '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)
      '/' -> {
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) {
            advance()
          }
        } else {
          addToken(SLASH)
        }
      }
      ' ', '\r', '\t' -> {
        // Ignore whitespace
      }
      '\n' -> line++
      '"' -> string()
      else -> when  {
        isDigit(c) -> number()
        isAlpha(c) -> identifier()
        else -> errorReporter.error(line, "Unexpected character.")
      }
    }
  }

  private fun identifier() {
    while (isAlphaNumeric(peek())) {
      advance()
    }
    val text = source.substring(start, current)
    val type = keywords.getOrDefault(text, IDENTIFIER)
    addToken(type)
  }

  private fun isAlphaNumeric(c: Char): Boolean {
    return isAlpha(c) || isDigit(c)
  }

  private fun isAlpha(c: Char): Boolean {
    return (c in 'a'..'z') || (c in 'A'..'Z') || (c == '_')
  }

  private fun number() {
    while (isDigit(peek())) {
      advance()
    }

    // Look for a fractional part.
    if (peek() == '.' && isDigit(peek(1))) {
      // Consume the "."
      advance();

      while (isDigit(peek())) {
        advance();
      }
    }

    addToken(
      TokenType.NUMBER,
      source.substring(start, current).toDouble()
    );
  }

  private fun isDigit(c: Char): Boolean {
    return c in '0'..'9'
  }

  private fun string() {
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') {
        line++
      }
      advance()
    }

    if (isAtEnd()) {
      errorReporter.error(line, "Unterminated string.")
      return
    }

    // The closing ".
    advance()

    // Trim the surrounding quotes.
    val value = source.substring(start + 1, current - 1)
    addToken(STRING, value)
  }

  private fun peek(delta :Int = 0): Char {
    if (current + delta >= source.length) {
      return '\u0000'
    }
    return source[current + delta]
  }

  private fun match(expected: Char): Boolean {
    if (isAtEnd()) {
      return false
    }
    if (source[current] != expected) {
      return false
    }
    current++
    return true
  }

  private fun addToken(type: TokenType) {
    addToken(type, null)
  }

  private fun addToken(type: TokenType, literal: Any?) {
    val text = source.substring(start, current)
    tokens.add(Token(type, text, literal, line))
  }

  private fun advance(): Char {
    current++
    return source[current-1]
  }

}
