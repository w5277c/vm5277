/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.Operator;

public class TOpearator extends Token {
	public TOpearator(Operator value, int endPos, int line, int column) {
		this.type = TokenType.OPERATOR;
		this.value = value;
		this.endPos = endPos;
		this.line = line;
		this.column = column;
	}

	public static TOpearator parse(String src, int pos, int line, int column) {
		
		// Проверяем трехсимвольные операторы (если есть)
        if (pos+2 < src.length()) {
            Operator op = Operator.fromSymbol(src.substring(pos, pos+3));
            if (null != op) {
				return new TOpearator(op, pos+3, line, column);
			}
        }
        
        // Проверяем двухсимвольные операторы
        if (pos+1 < src.length()) {
            Operator op = Operator.fromSymbol(src.substring(pos, pos+2));
            if (null != op) {
				return new TOpearator(op, pos+2, line, column);
			}
        }
        
        // Проверяем односимвольные операторы
        Operator op = Operator.fromSymbol(src.substring(pos, pos+1));
		if (null != op) {
			return new TOpearator(op, pos+1, line, column);
		}
		return null;
	}
}
