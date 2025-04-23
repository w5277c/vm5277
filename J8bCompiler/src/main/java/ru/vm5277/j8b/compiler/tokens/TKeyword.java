/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.tokens.enums.TokenType;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;

public class TKeyword extends Token {
	public TKeyword(String src, int pos, int line, int column) {
		endPos = pos;
		this.line = line;
		this.column = column;
		
		StringBuilder sb = new StringBuilder();
        while (endPos<src.length() && (Character.isLetterOrDigit(src.charAt(endPos)) || '_'==src.charAt(endPos))) {
            sb.append(src.charAt(endPos));
			endPos++;
			column++;
        }
        String id = sb.toString();
        Keyword keyword = Keyword.fromString(id);
		
		if(null != keyword) {
			type = Keyword.getTokenType(keyword);
			value = keyword;
		}
		else {
			type = TokenType.ID;
			value = id;
		}
	}
}
