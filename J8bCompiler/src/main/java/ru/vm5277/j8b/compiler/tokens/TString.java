/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.messages.MessageContainer;

public class TString extends Token {
	public TString(SourceBuffer sb, MessageContainer mc) {
		super(sb);
		type = TokenType.STRING;
		
		StringBuilder str = new StringBuilder();
		sb.next(); // Пропускаем '"'
		while (sb.hasNext() && '"'!=sb.getChar()) {
			str.append(sb.getChar());
			sb.next();			
        }
		if (!sb.hasNext()) {
			setError("Unterminated string literal", mc);
		}
		else {
			sb.next();
		}
        value = str.toString();
	}
}
