/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens.enums;

public enum Delimiter {
    // Группировка
    LEFT_PAREN("(", "Открывающая скобка"),
    RIGHT_PAREN(")", "Закрывающая скобка"),
    LEFT_BRACE("{", "Открывающая фигурная скобка"),
    RIGHT_BRACE("}", "Закрывающая фигурная скобка"),
    LEFT_BRACKET("[", "Открывающая квадратная скобка"),
    RIGHT_BRACKET("]", "Закрывающая квадратная скобка"),

    // Окончания
    SEMICOLON(";", "Конец выражения"),
    COMMA(",", "Разделитель аргументов/элементов"),
    COLON(":", "Разделитель в тернарном операторе/метках"),

    // Специальные
    DOT(".", "Доступ к полям/методам"),
    ELLIPSIS("...", "Varargs"),
    AT("@", "Аннотации");
    
	private final String symbol;
	private final String description;

    Delimiter(String symbol, String description) {
        this.symbol = symbol;
        this.description = description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

    public static Delimiter fromSymbol(String symbol) {
        // Сначала проверяем многозначные разделители
        for (Delimiter delim : values()) {
            if (delim.symbol.equals(symbol)) {
                return delim;
            }
        }
        return null;
    }

    /**
     * Проверка, является ли символ началом разделителя
     */
    public static boolean isDelimiterStart(char ch) {
        return ch == '(' || ch == '{' || ch == '[' ||
               ch == ')' || ch == '}' || ch == ']' ||
               ch == ';' || ch == ',' || ch == ':' ||
               ch == '.' || ch == '@';
    }

    /**
     * Получить самый длинный возможный разделитель с текущей позиции
     */
    public static Delimiter matchLongestDelimiter(String input, int pos) {
        // Проверяем трехсимвольные (только ELLIPSIS)
        if (pos + 2 < input.length()) {
            String threeChar = input.substring(pos, pos + 3);
            if (threeChar.equals(ELLIPSIS.symbol)) {
                return ELLIPSIS;
            }
        }

        // Проверяем односимвольные
        String singleChar = input.substring(pos, pos + 1);
        return fromSymbol(singleChar);
    }
}