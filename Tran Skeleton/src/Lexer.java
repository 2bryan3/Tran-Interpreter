import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.lang.Exception;


public class Lexer {
    private TextManager newManager;
    int lineNumber = 1;
    int characterPosition = 0;
    int previousIndentLevel, currentIndentLevel;

    HashMap<String, Token.TokenTypes> keywords = new HashMap<>();
    HashMap<String, Token.TokenTypes> punctuation = new HashMap<>();

    public Lexer(String input) {
        newManager = new TextManager(input);

        keywords.put("accessor:", Token.TokenTypes.ACCESSOR);
        keywords.put("accessor :", Token.TokenTypes.ACCESSOR);
        keywords.put("mutator:", Token.TokenTypes.MUTATOR);
        keywords.put("mutator :", Token.TokenTypes.MUTATOR);
        keywords.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywords.put("class", Token.TokenTypes.CLASS);
        keywords.put("interface", Token.TokenTypes.INTERFACE);
        keywords.put("loop", Token.TokenTypes.LOOP);
        keywords.put("if", Token.TokenTypes.IF);
        keywords.put("else", Token.TokenTypes.ELSE);
        keywords.put("true", Token.TokenTypes.TRUE);
        keywords.put("new", Token.TokenTypes.NEW);
        keywords.put("false", Token.TokenTypes.FALSE);
        keywords.put("private", Token.TokenTypes.PRIVATE);
        keywords.put("shared", Token.TokenTypes.SHARED);
        keywords.put("construct", Token.TokenTypes.CONSTRUCT);


        punctuation.put("=", Token.TokenTypes.ASSIGN);
        punctuation.put("(", Token.TokenTypes.LPAREN);
        punctuation.put(")", Token.TokenTypes.RPAREN);
        punctuation.put(":", Token.TokenTypes.COLON);
        punctuation.put(".", Token.TokenTypes.DOT);
        punctuation.put("+", Token.TokenTypes.PLUS);
        punctuation.put("-", Token.TokenTypes.MINUS);
        punctuation.put("*", Token.TokenTypes.TIMES);
        punctuation.put("/", Token.TokenTypes.DIVIDE);
        punctuation.put("%", Token.TokenTypes.MODULO);
        punctuation.put(",", Token.TokenTypes.COMMA);
        punctuation.put("==", Token.TokenTypes.EQUAL);
        punctuation.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuation.put("<", Token.TokenTypes.LESSTHAN);
        punctuation.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuation.put(">", Token.TokenTypes.GREATERTHAN);
        punctuation.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
        punctuation.put("!", Token.TokenTypes.NOT);
        punctuation.put("&&", Token.TokenTypes.AND);
        punctuation.put("||", Token.TokenTypes.OR);

        keywords.put("\n", Token.TokenTypes.NEWLINE);

    }
    //TODO comment code for future reference
    public List<Token> Lex() throws Exception {
        var retVal = new LinkedList<Token>();
        String currentWord = "";
        int totalIndents = 0;
        while (!newManager.isAtEnd()) {
            char currentCharacter = newManager.peekCharacter();
            Token newToken;
            if (Character.isLetter(currentCharacter)) {
                retVal.add(parseWord());
                continue;
            } else if (Character.isDigit(currentCharacter)) {
                retVal.add(parseNumber());
                continue;
            } else if (currentCharacter == '.' && (Character.isDigit(newManager.peekCharacter(1)))){
                retVal.add(parseNumber());
            } else if( currentCharacter == '.') {
                currentWord += newManager.getCharacter();
                newToken = new Token(Token.TokenTypes.DOT, lineNumber, characterPosition);
                retVal.add(newToken);
                currentWord = "";
            }
            else if (currentCharacter == '-'){
                currentWord += newManager.getCharacter();
                newToken = new Token(Token.TokenTypes.MINUS,lineNumber,characterPosition);
                retVal.add(newToken);
                currentWord = "";
            } else if (currentCharacter == '\n') {
                lineNumber ++;
                newManager.getCharacter();
                while(!newManager.isAtEnd() && newManager.peekCharacter() == '\n') {
                    newManager.getCharacter();
                    lineNumber ++;
                    characterPosition = 0;
                }
                characterPosition = 0;
                previousIndentLevel = currentIndentLevel;
                currentIndentLevel = 0;
                currentWord += currentCharacter;
                newToken = new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition, currentWord);
                retVal.add(newToken);
                currentWord = "";
                if (!newManager.isAtEnd()) {
                    if (newManager.peekCharacter() == ' ' || newManager.peekCharacter() == '\t') {
                        currentCharacter = newManager.peekCharacter();
                        if (characterPosition == 0) {
                            int k = 0;
                            for (int i = 0; currentCharacter == '\t' || currentCharacter == ' '; i++) {
                                currentCharacter = newManager.peekCharacter(i);
                                if (currentCharacter == '\t') {
                                    currentIndentLevel += 4;
                                    characterPosition += 4;
                                    k++;
                                } else if (currentCharacter == ' ') {
                                    characterPosition++;
                                    currentIndentLevel++;
                                    k++;
                                }
                                if(currentCharacter == '\n'){
                                    for(int j = 0; j < k; j++){
                                        newManager.getCharacter();
                                    }
                                    newManager.getCharacter();
                                }
                            }
                            if (indentationCounter() > 0) {
                                retVal.add(parserIndentation());
                                totalIndents++;
                            } else if (indentationCounter() < 0) {
                                retVal.add(parserDedentation());
                                totalIndents--;
                            }
                        }
                    } else if (indentationCounter() < 0) {
                        retVal.add(parserDedentation());
                        totalIndents--;
                    }
                }
            } else if (currentCharacter == '\'') {
                retVal.add(quotedCharacterChecker());
            } else if (currentCharacter == '\"') {
                retVal.add(quotedStringChecker());
            } else if (Character.isWhitespace(currentCharacter)) {
                newManager.getCharacter();
                characterPosition++;
            } else if (currentCharacter == '{') {
                if (!commentChecker()) {
                    throw new SyntaxErrorException("The quantity of open and closed curly brackets is not equal", lineNumber,characterPosition);
                }
            } else {
                retVal.add(parsePunctuation());
                continue;
            }
        } if (totalIndents > 0){
            for (int i = 0; i < totalIndents; i++) {
                retVal.add(parserDedentation());
            }
        }
        return retVal;
    }

    public Token parseWord() {
        String currentWord = "";
        boolean hasSeenQuotedString = false;
        while (!newManager.isAtEnd()) {
            char currentCharacter = newManager.peekCharacter();
            if (Character.isLetter(currentCharacter)) {
                currentCharacter = newManager.getCharacter();
            }
            if (currentCharacter == '\n') {
                if (!currentWord.isEmpty()) {
                    if (keywords.containsKey(currentWord)) {
                        return new Token(keywords.get(currentWord), lineNumber, characterPosition);
                    } else {
                        return new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, currentWord);
                    }
                }
                return new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition, currentWord);
            }
            if (!Character.isLetter(currentCharacter)) {
                if ((currentCharacter == ':' & !currentWord.isEmpty()) & (currentWord.equals("mutator") || currentWord.equals("accessor"))) {
                    currentWord += currentCharacter;
                } else if (currentCharacter == ' ' & (currentWord.equals("mutator") || currentWord.equals("accessor"))) {
                    currentWord += currentCharacter;
                    currentCharacter = newManager.peekCharacter(1);
                    if (currentCharacter == ':') {
                        currentWord += currentCharacter;
                    }
                }
                if (!currentWord.isEmpty()) {
                    if (keywords.containsKey(currentWord)) {
                        return new Token(keywords.get(currentWord), lineNumber, characterPosition);
                    } else {
                        return new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, currentWord);
                    }
                }
            } else {
                currentWord += currentCharacter;
                characterPosition++;
            }
        }
        if (!currentWord.isEmpty()) {
            if (keywords.containsKey(currentWord)) {
                return new Token(keywords.get(currentWord), lineNumber, characterPosition);
            }
            return new Token(Token.TokenTypes.WORD, lineNumber, characterPosition, currentWord);
        }
        return null;
    }

    public Token parseNumber() throws SyntaxErrorException {
        var retVal = new LinkedList<Token>();
        String currentNumber = "";
        boolean hasSeenPeriod = false;
        while (!newManager.isAtEnd()) {
            char currentCharacter = newManager.peekCharacter();
            if (currentCharacter == '.' && !hasSeenPeriod) {
                currentNumber += newManager.getCharacter();
                hasSeenPeriod = true;
                continue;
            }
            if (!Character.isDigit(currentCharacter) & !hasSeenPeriod) {
                if (!currentNumber.isEmpty()) {
                    return new Token(Token.TokenTypes.NUMBER, lineNumber, currentCharacter, currentNumber);
                }
            } else if (!Character.isDigit(currentCharacter) & hasSeenPeriod) {
                if (currentCharacter == '.') {
                    throw new SyntaxErrorException("System Error: Can only have one period at a time", lineNumber, characterPosition);
                }
                else {
                    return new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentNumber);
                }
            }
            currentNumber += newManager.getCharacter();
        }
        return new Token(Token.TokenTypes.NUMBER, lineNumber, characterPosition, currentNumber);
    }

    public Token parsePunctuation() throws SyntaxErrorException {
        String currentWord = "";
        while (!newManager.isAtEnd()) {
            char currentCharacter = newManager.peekCharacter();
            Token newToken;
            if (!Character.isLetter(currentCharacter)) {
                if (!Character.isDigit(currentCharacter)) {
                    if(currentCharacter != ' '){
                        currentWord += newManager.getCharacter();
                    }
                    if(!newManager.isAtEnd()) {
                        if (newManager.peekCharacter() == '=' || newManager.peekCharacter() == '&' || newManager.peekCharacter() == '|') {
                            currentWord += newManager.peekCharacter();
                            if (punctuation.containsKey(currentWord)) {
                                newManager.getCharacter();
                                return new Token(punctuation.get(currentWord), lineNumber, characterPosition, currentWord);
                            } else {
                                throw new SyntaxErrorException("System Error: That is not a valid punctuation", lineNumber, characterPosition);
                            }
                        } else if (punctuation.containsKey(currentWord)) {
                            return new Token(punctuation.get(currentWord), lineNumber, characterPosition, currentWord);
                        } else {
                            throw new SyntaxErrorException("System Error: That is not a valid punctuation", lineNumber, characterPosition);
                        }
                    } if (punctuation.containsKey(currentWord)) {
                        return new Token(punctuation.get(currentWord), lineNumber, characterPosition, currentWord);
                    } else {
                        throw new SyntaxErrorException("System Error: That is not a valid punctuation",lineNumber,characterPosition);
                    }
                }
            }
        }
        return null;
    }

    public Token parserIndentation() throws SyntaxErrorException {
        return new Token(Token.TokenTypes.INDENT,lineNumber,characterPosition);
    }

    public Token parserDedentation() throws SyntaxErrorException {
        return new Token(Token.TokenTypes.DEDENT,lineNumber,characterPosition);
    }

    public int indentationCounter() throws SyntaxErrorException {
        if (currentIndentLevel % 4 != 0){
            throw new SyntaxErrorException("System error: wrong number of indentations", lineNumber, characterPosition);
        }
        if (currentIndentLevel > previousIndentLevel) {
            return currentIndentLevel - previousIndentLevel;
        } else if(currentIndentLevel < previousIndentLevel) {
            return currentIndentLevel - previousIndentLevel;
        }
        return 0;
    }

    public boolean commentChecker(){
        int openCurlyBraceCount = 0;
        int closeCurlyBraceCount = 0;

        while(!newManager.isAtEnd()) {
            char currentCharacter = newManager.getCharacter();

            if (currentCharacter == '{') {
                openCurlyBraceCount++;
            } else if (currentCharacter == '}') {
                closeCurlyBraceCount++;
            } else if (currentCharacter == '\n') {
                lineNumber++;
            }
        }
        return openCurlyBraceCount == closeCurlyBraceCount;
    }

    public Token quotedStringChecker() throws SyntaxErrorException {
        int numberOfCharactersSeen = 0;
        int numberOfQuotesSeen = 0;
        String currentString = "";
        var retVal = new LinkedList<Token>();

        while (!newManager.isAtEnd()) {
            char currentCharacter = newManager.peekCharacter();
            if (currentCharacter == '\"') {
                numberOfQuotesSeen++;
                currentCharacter = newManager.getCharacter();
            } else if (currentCharacter == '\n') {
                lineNumber++;
                newManager.getCharacter();
            } else{
                currentString += currentCharacter;
                currentCharacter = newManager.getCharacter();
                numberOfCharactersSeen++;
            }
            if (numberOfQuotesSeen % 2 == 0){
                return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, characterPosition,currentString);
            }
        }
        if (numberOfQuotesSeen % 2 == 1) {
            throw new SyntaxErrorException("System error: odd number of quotes found",lineNumber,characterPosition);
        }
        return null;
    }

    public Token quotedCharacterChecker() throws SyntaxErrorException {
        int numberOfCharactersSeen = 0;
        int numberOfQuotesSeen = 0;
        String currentString = "";

        while (!newManager.isAtEnd()) {
            char currentCharacter = newManager.peekCharacter();
            if (currentCharacter == '\'') {
                currentCharacter = newManager.getCharacter();
                numberOfQuotesSeen++;
            } else {
                currentString += currentCharacter;
                newManager.getCharacter();
                numberOfCharactersSeen++;
            }
            if (numberOfCharactersSeen == 1 && numberOfQuotesSeen == 2) {
                return new Token(Token.TokenTypes.QUOTEDCHARACTER, lineNumber, characterPosition, currentString);
            }
        }
        if (numberOfQuotesSeen % 2 == 1 || numberOfCharactersSeen > 1) {
            throw new SyntaxErrorException("System error: too many characters for single quotes", lineNumber, characterPosition);
        }
        return null;
    }
}