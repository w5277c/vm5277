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
package ru.vm5277.compiler;

import ru.vm5277.common.SourceBuffer;

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
    RANGE("..", "Диапазон"),
	ELLIPSIS("...", "Varargs");
    
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

	@Override
	public String toString() {
		return symbol;
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

    // Проверка, является ли символ началом разделителя
    public static boolean isDelimiterStart(char ch) {
        return ch == '(' || ch == '{' || ch == '[' ||
               ch == ')' || ch == '}' || ch == ']' ||
               ch == ';' || ch == ',' || ch == ':' ||
               ch == '.' || ch == '@';
    }

    // Получить самый длинный возможный разделитель с текущей позиции
    public static Delimiter matchLongestDelimiter(SourceBuffer sb) {
        // Проверяем трехсимвольные (только ELLIPSIS)
        if (sb.hasNext(2)) {
            String threeChar = sb.getSource().substring(sb.getPos(), sb.getPos()+3);
            if (threeChar.equals(ELLIPSIS.symbol)) {
                sb.next(3);
				return ELLIPSIS;
            }
        }
		// Проверяем двухсимвольные (только RANGE)
        if (sb.hasNext(1)) {
            String twoChar = sb.getSource().substring(sb.getPos(), sb.getPos()+2);
            if (twoChar.equals(RANGE.symbol)) {
                sb.next(2);
				return RANGE;
            }
        }
        // Проверяем односимвольные
        String singleChar = sb.getSource().substring(sb.getPos(), sb.getPos()+1);
		Delimiter result = fromSymbol(singleChar);
		if(null != result) sb.next();
		return result;
    }
}