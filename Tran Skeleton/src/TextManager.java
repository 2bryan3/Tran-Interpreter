public class TextManager {
    private String text;
    private int position;

    public TextManager(String input) {
        position = 0;
        text = input;
    }

    public boolean isAtEnd() {
            if (position == text.length()) {
                return true;
            }
            return false;
    }

    public char peekCharacter() {
            char charPosition = text.charAt(position);
            return charPosition;
    }

    public char peekCharacter(int distance) {
        char charPosition = text.charAt(distance + position);
        return charPosition;
    }

    public char getCharacter() {
        char charPosition = text.charAt(position);
        position++;
        return charPosition;
    }
}
