/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.tokens;

import ru.vm5277.common.SourceBuffer;
import ru.vm5277.j8b_compiler.TokenType;
import ru.vm5277.j8b_compiler.Keyword;

public class TKeyword extends Token {
	public TKeyword(SourceBuffer sb) {
		super(sb);
		
		StringBuilder stringBuilder = new StringBuilder();
        while (sb.hasNext() && (Character.isLetterOrDigit(sb.getChar()) || '_'==sb.getChar())) {
            stringBuilder.append(sb.getChar());
			sb.next();
        }
        String id = stringBuilder.toString();
        Keyword keyword = Keyword.fromString(id);
		
		if(null != keyword) {
			type = keyword.getTokenType();
			value = keyword;
		}
		else {
			type = TokenType.ID;
			value = id;
		}
	}
}
