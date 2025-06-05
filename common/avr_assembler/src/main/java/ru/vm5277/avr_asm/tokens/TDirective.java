/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.tokens;

import ru.vm5277.common.SourceBuffer;
import ru.vm5277.common.Keyword;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.tokens.Token;

public class TDirective extends Token {
	public TDirective(SourceBuffer sb, MessageContainer mc) {
		super(sb);
		
		StringBuilder stringBuilder = new StringBuilder(".");
        while (sb.hasNext() && (Character.isLetterOrDigit(sb.getChar()))) {
            stringBuilder.append(sb.getChar());
			sb.next();
        }
        String id = stringBuilder.toString();
        Keyword keyword = Keyword.fromString(id.toLowerCase());
		
		if(null != keyword) {
			type = keyword.getTokenType();
			value = keyword;
		}
		else {
			setError("Unsupported directive:" + id, mc);
		}
	}
}
