/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.common.lexer;

public enum Delimiter {
    // Группировка
    LEFT_PAREN("(", "Открывающая скобка", LexerType.ALL),
    RIGHT_PAREN(")", "Закрывающая скобка", LexerType.ALL),
    LEFT_BRACE("{", "Открывающая фигурная скобка", LexerType.J8B),
    RIGHT_BRACE("}", "Закрывающая фигурная скобка", LexerType.J8B),
    LEFT_BRACKET("[", "Открывающая квадратная скобка", LexerType.J8B),
    RIGHT_BRACKET("]", "Закрывающая квадратная скобка", LexerType.J8B),

    // Окончания
    SEMICOLON(";", "Конец выражения", LexerType.J8B),
    COMMA(",", "Разделитель аргументов/элементов", LexerType.ALL),
    COLON(":", "Разделитель в тернарном операторе/метках", LexerType.J8B),

    // Специальные
    DOT(".", "Доступ к полям/методам", LexerType.J8B),
    RANGE("..", "Диапазон", LexerType.J8B),
	ELLIPSIS("...", "Varargs", LexerType.J8B);
    
	private	final	String		symbol;
	private	final	String		description;
	private			LexerType	lexerType;
	
    Delimiter(String symbol, String description, LexerType lexerType) {
        this.symbol = symbol;
        this.description = description;
		this.lexerType = lexerType;
    }

	public String getSymbol() {
        return symbol;
    }

    public String getDescription() {
        return description;
    }

	public LexerType getLexerType() {
		return lexerType;
	}
	
	@Override
	public String toString() {
		return symbol;
	}
	
    // Проверка, является ли символ началом разделителя
    public static boolean isDelimiterStart(char ch) {
        return ch == '(' || ch == '{' || ch == '[' ||
               ch == ')' || ch == '}' || ch == ']' ||
               ch == ';' || ch == ',' || ch == ':' ||
               ch == '.' || ch == '@';
    }

    // Получить самый длинный возможный разделитель с текущей позиции
    public static Delimiter matchDelimiter(SourceBuffer sb, LexerType lexerType) {
		if(sb.available()) {
			if(LexerType.J8B==lexerType) {
				if(sb.consumeIfEqual(ELLIPSIS.symbol)) {
					return ELLIPSIS;
				}
				if(sb.consumeIfEqual(RANGE.symbol)) {
					return RANGE;
				}
			}

			char c = sb.peek();
			for(Delimiter delim : values()) {
				if(delim.getLexerType()==lexerType || LexerType.ALL==delim.getLexerType()) {
					if(delim.symbol.charAt(0)==c) {
						sb.next();
						return delim;
					}
				}
			}
		}
		
		return null;
    }
}