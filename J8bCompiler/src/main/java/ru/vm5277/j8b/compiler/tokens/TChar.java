/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.messages.MessageContainer;

public class TChar extends Token {
	
	public TChar(SourceBuffer sb, MessageContainer mc) {
		super(sb);
		type = TokenType.CHAR;

		if (!sb.hasNext()) {
			setError("Unterminated ASCII char literal", mc);
			value = '?';
			return;
		}
		sb.next();
		char ch = sb.getChar(); // Пропускаем открывающую кавычку
		// Обработка экранированных символов (\n, \t, \', \\)
		if ('\\'==ch) {
			if (!sb.hasNext()) {
				setError("Invalid escape sequence", mc);
				value = '?';
				return;
			}
			sb.next();
			ch = sb.getChar();
			switch (ch) {
				case 'n':	ch = '\n';	break;
				case 't':	ch = '\t';	break;
				case '\'':	ch = '\'';	break;
				case '\\':	ch = '\\';	break;
				case '0':	ch = '\0';	break;
				default:
					setError("Unknown escape: \\" + ch, mc);
					ch = '?';
			}
	    }
    
		// Пропускаем символ и проверяем закрывающую кавычку '
		sb.next();
		if (!sb.hasNext() || '\'' != sb.getChar()) {
			setError("Char literal must be 1 ASCII character", mc);
		}
		else {
			sb.next();
		}
		value = String.valueOf(ch);
	}
}
