/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class TString extends Token {
	public TString(String src, int pos, int line, int column) {
		type = TokenType.STRING;
		
		endPos = pos;
		this.line = line;
		this.column = column;
		
		StringBuilder str = new StringBuilder();
		endPos++; // Пропускаем открывающую кавычку
		column++;
		while (endPos<src.length() && '"'!=src.charAt(endPos)) {
			str.append(src.charAt(endPos));
			endPos++;
			column++;
        }
		if (endPos>=src.length()) {
            throw new ParseError("Unterminated string literal", line, this.column);
		}
		endPos++; // Пропускаем закрывающую кавычку
        value = str.toString();
	}
}
