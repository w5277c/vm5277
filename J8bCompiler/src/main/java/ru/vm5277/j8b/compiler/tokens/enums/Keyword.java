/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens.enums;

public enum Keyword {
	// Литералы
	TRUE, FALSE, NULL,
	// Типы примитив
	VOID, BOOL, BYTE, SHORT, INT, FIXED,
	// Команды
	IF, ELSE, WHILE, FOR, RETURN,
	// Модификаторы
	STATIC, FINAL, PRIVATE, PUBLIC, NATIVE, ATOMIC,
	// Ключевые слова ООП
	CLASS, INTERFACE, IMPLEMENTS,
	//Остальное
	IMPORT, AS;
	
	public static Keyword fromString(String str) {
		try {
			return Keyword.valueOf(str.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static TokenType getTokenType(Keyword keyword) {
		switch(keyword) {
			case TRUE:
			case FALSE:
			case NULL:
				return TokenType.LITERAL;
			case VOID:
			case BOOL:
			case BYTE:
			case SHORT:
			case INT:
			case FIXED:
				return TokenType.TYPE;
			case IF:
			case ELSE:
			case WHILE:
			case FOR:
			case RETURN:
				return TokenType.COMMAND;
			case STATIC:
			case FINAL:
			case PRIVATE:
			case PUBLIC:
				return TokenType.MODIFIER;
			case CLASS:
			case IMPLEMENTS:
			case INTERFACE:
				return TokenType.OOP;
			default:
				return TokenType.KEYWORD;
		}
	}
}
