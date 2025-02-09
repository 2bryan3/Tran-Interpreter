import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;

public class Lexer1ExtraTests {

    @Test
    public void SimpleLexerTest() {
        var l = new Lexer("ab cd ef 1 12 1.3 1.44");
        try {
            var res = l.Lex();
            Assertions.assertEquals(7, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("ab", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(1).getType());
            Assertions.assertEquals("cd", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("1", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(4).getType());
            Assertions.assertEquals("12", res.get(4).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(5).getType());
            Assertions.assertEquals("1.3", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(6).getType());
            Assertions.assertEquals("1.44", res.get(6).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MultilineLexerTest() {
        var l = new Lexer("\n\nef gh\nasdtvjkl\ndsajkdsa kschf \n");
        try {
            var res = l.Lex();
            Assertions.assertEquals(10, res.size());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(2).getType());
            Assertions.assertEquals("ef", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(3).getType());
            Assertions.assertEquals("gh", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(5).getType());
            Assertions.assertEquals("asdtvjkl", res.get(5).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(7).getType());
            Assertions.assertEquals("dsajkdsa", res.get(7).getValue());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(8).getType());
            Assertions.assertEquals("kschf", res.get(8).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(9).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void GreaterThanEqualTest() {
        var l = new Lexer(">=");
        try {
            var res = l.Lex();
            Assertions.assertEquals(1, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void TwoCharacterTest() {
        var l = new Lexer(">= : <= < = == != ! && ||");
        try {
            var res = l.Lex();
            Assertions.assertEquals(10, res.size());
            Assertions.assertEquals(Token.TokenTypes.GREATERTHANEQUAL, res.get(0).getType());
            Assertions.assertEquals(Token.TokenTypes.COLON, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHANEQUAL, res.get(2).getType());
            Assertions.assertEquals(Token.TokenTypes.LESSTHAN, res.get(3).getType());
            Assertions.assertEquals(Token.TokenTypes.ASSIGN, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.EQUAL, res.get(5).getType());
            Assertions.assertEquals(Token.TokenTypes.NOTEQUAL, res.get(6).getType());
            Assertions.assertEquals(Token.TokenTypes.NOT, res.get(7).getType());
            Assertions.assertEquals(Token.TokenTypes.AND, res.get(8).getType());
            Assertions.assertEquals(Token.TokenTypes.OR, res.get(9).getType());

        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void MixedTest() {
        var l = new Lexer("Hello \n1.44 9 + , ");
        try {
            var res = l.Lex();
            Assertions.assertEquals(6, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("Hello", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.NEWLINE, res.get(1).getType());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(2).getType());
            Assertions.assertEquals("1.44", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("9", res.get(3).getValue());
            Assertions.assertEquals(Token.TokenTypes.PLUS, res.get(4).getType());
            Assertions.assertEquals(Token.TokenTypes.COMMA, res.get(5).getType());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }
}