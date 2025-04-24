/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.Delimiter;

public class TDelimiter extends Token {
	public TDelimiter(Delimiter value, int endPos, int line, int column) {
		this.type = TokenType.DELIMITER;
		this.value = value;
		this.endPos = endPos;
		this.line = line;
		this.column = column;
	}

	public static TDelimiter parse(String src, int pos, int line, int column) {

		int endPos = pos;
		
		Delimiter delim = Delimiter.matchLongestDelimiter(src, endPos);
		if (null!=delim) {
			endPos += delim.getSymbol().length();
			column += delim.getSymbol().length();
			return new TDelimiter(delim, endPos, line, column);
		}
	    return null;
	}
}
