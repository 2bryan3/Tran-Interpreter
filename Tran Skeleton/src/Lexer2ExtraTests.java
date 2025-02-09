import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Lexer2ExtraTests {
    @Test
    public void CommentLexerTest() {
        var l = new Lexer("{ inside of this curly brackets is a comment }");
        try {
            var res = l.Lex();
            Assertions.assertEquals(0, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void CommentLexerTest2() {
        var l = new Lexer("{ inside of this {curly brackets} is a comment}");
        try {
            var res = l.Lex();
            Assertions.assertEquals(0, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void QuotedStringLexerTest() {
        var l = new Lexer("test \"hello \nthere \nhow are you\" \'t\' 1.2");
        try {
            var res = l.Lex();
            Assertions.assertEquals(4, res.size());
            Assertions.assertEquals(Token.TokenTypes.WORD, res.get(0).getType());
            Assertions.assertEquals("test", res.get(0).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDSTRING, res.get(1).getType());
            Assertions.assertEquals("hello there how are you", res.get(1).getValue());
            Assertions.assertEquals(Token.TokenTypes.QUOTEDCHARACTER, res.get(2).getType());
            Assertions.assertEquals("t", res.get(2).getValue());
            Assertions.assertEquals(Token.TokenTypes.NUMBER, res.get(3).getType());
            Assertions.assertEquals("1.2", res.get(3).getValue());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    public void IndentTest() {
        var l = new Lexer(
                "loop keepGoing\n" +
                        "\tif n >= 15\n" +
                        "\t\tkeepGoing\t = false\n"
        );
        try {
            var res = l.Lex();
            Assertions.assertEquals(16, res.size());
        }
        catch (Exception e) {
            Assertions.fail("exception occurred: " +  e.getMessage());
        }
    }

    @Test
    void exceptionTesting() {
        Throwable exception = Assertions.assertThrows(SyntaxErrorException.class, () -> {
            throw new SyntaxErrorException("Error", 0,0);
        });
        Assertions.assertEquals("Error", exception.getMessage());
    }
}