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
package ru.vm5277.compiler.tokens;

import ru.vm5277.common.SourceBuffer;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.messages.MessageContainer;

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
		char ch = sb.getChar(); // Пропускаем '''
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
    
		// Пропускаем символ и проверяем '''
		sb.next();
		if (!sb.hasNext() || '\'' != sb.getChar()) {
			setError("Char literal must be 1 ASCII character", mc);
		}
		else {
			sb.next();
		}
		value = ch;
	}
}
