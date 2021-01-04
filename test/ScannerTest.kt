import org.junit.jupiter.api.Test

import TokenType.*
import org.junit.jupiter.api.Assertions.assertEquals

internal class ScannerTest {
  @Test
  fun basicLine() {
    val source = "var i = 123;"
    val scanner = Scanner(source)
    val expected = listOf(
      Token(VAR, "var", null, 1),
      Token(IDENTIFIER, "i", null, 1),
      Token(EQUAL, "=", null, 1),
      Token(NUMBER, "123", 123.0, 1),
      Token(SEMICOLON, ";", null, 1),
      Token(EOF, "", null, 1)
    )
    assertEquals(expected, scanner.scanTokens())
  }

  @Test
  fun string() {
    val source = """var st = "my string";"""
    val scanner = Scanner(source)
    val expected = listOf(
      Token(VAR, "var", null, 1),
      Token(IDENTIFIER, "st", null, 1),
      Token(EQUAL, "=", null, 1),
      Token(STRING, "\"my string\"", "my string", 1),
      Token(SEMICOLON, ";", null, 1),
      Token(EOF, "", null, 1)
    )
    assertEquals(expected, scanner.scanTokens())
  }

  @Test
  fun multilineString() {
    val str = """
      my
      long
      string
    """.trimIndent()
    val source = "var st = \"$str\";"
    val scanner = Scanner(source)
    val expected = listOf(
      Token(VAR, "var", null, 1),
      Token(IDENTIFIER, "st", null, 1),
      Token(EQUAL, "=", null, 1),
      Token(STRING, "\"$str\"", str, 3),
      Token(SEMICOLON, ";", null, 3),
      Token(EOF, "", null, 3)
    )
    assertEquals(expected, scanner.scanTokens())
  }

}