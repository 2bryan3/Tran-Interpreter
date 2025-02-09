import java.util.List;
import java.util.Optional;

public class TokenManager {
    private int position = 0;
    private List<Token> tokens;

    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean done() {
        if (position == tokens.size()) {
            return true;
        }
        return false;
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes tokenType) {
        if(position < tokens.size()) {
            if (tokenType == tokens.get(position).getType()) {
                return Optional.of(tokens.get(position++));
            }
        }
        return Optional.empty();
    }

    public Optional<Token> peekCurrent() {
        if(position < tokens.size()) {
            return Optional.of(tokens.get(position));
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if(position < tokens.size()) {
            return Optional.of(tokens.get(i));
        }
        return Optional.empty();
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second){
        if(first == tokens.get(position).getType()) {
            if(second == tokens.get(position + 1).getType()) {
                return true;
            }
        }
        return false;
    }

    public int getCurrentLine(){
        return tokens.get(position).getLineNumber();
    };
    public int getCurrentColumnNumber(){
        return tokens.get(position).getColumnNumber();
    };
}