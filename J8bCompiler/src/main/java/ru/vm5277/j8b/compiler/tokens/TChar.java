/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class TChar extends Token {
	public TChar(String src, int pos, int line, int column) {
		type = TokenType.CHAR;
		
		endPos = pos;
		this.line = line;
		this.column = column;

		endPos++; // Пропускаем открывающую кавычку
		column++;
		if (endPos >= src.length()) {
			throw new ParseError("Unterminated ASCII char literal", line, column);
		}
    
		char ch = src.charAt(endPos);
    
		// Обработка экранированных символов (\n, \t, \', \\)
		if ('\\'==ch) {
			endPos++;
			column++;
			if (endPos >= src.length()) {
				throw new ParseError("Invalid escape sequence", line, column);
			}
			
			ch = src.charAt(endPos);
			switch (ch) {
				case 'n':	ch = '\n';	break;
				case 't':	ch = '\t';	break;
				case '\'':	ch = '\'';	break;
				case '\\':	ch = '\\';	break;
				case '0':	ch = '\0';	break;
				default:
					throw new ParseError("Unknown escape: \\" + ch, line, column);
			}
	    }
    
		// Пропускаем символ и проверяем закрывающую кавычку '
		endPos++;
		column++;
		if (endPos >= src.length() || '\'' != src.charAt(endPos)) {
	        throw new ParseError("Char literal must be 1 ASCII character", line, column);
		}
		endPos++;
		column++;
    
		value = ch;
	}
}
