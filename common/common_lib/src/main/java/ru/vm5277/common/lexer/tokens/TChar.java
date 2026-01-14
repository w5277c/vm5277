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

package ru.vm5277.common.lexer.tokens;

import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;

public class TChar extends Token {
	
	public TChar(SourceBuffer sb) throws CompileException {
		super(sb);
		
		type = TokenType.CHARACTER;

		if(!sb.available()) {
			throw new CompileException("Unterminated character literal");
		}
		sb.next();
		char ch = sb.peek(); // Пропускаем '''
		// Обработка экранированных символов (\n, \t, \', \\)
		if('\\'==ch) {
			if(!sb.available()) {
				throw new CompileException("Incomplete escape sequence");
			}
			sb.next();
			ch = sb.peek();
			switch (ch) {
				case 'n':	ch = '\n';	break;
				case 't':	ch = '\t';	break;
				case '\'':	ch = '\'';	break;
				case '\\':	ch = '\\';	break;
				case '0':	ch = '\0';	break;
				default:
					skipToken(sb);
					throw new CompileException("Unknown escape: \\" + ch);
			}
	    }
    
		// Пропускаем символ и проверяем '''
		sb.next();
		if(!sb.available()) {
			throw new CompileException("Unterminated character literal");
		}
		else if('\''!= sb.peek()) {
			sb.next();
			throw new CompileException("Character literals must be enclosed in single quotes");
		}
		else {
			sb.next();
		}
		value = ch;
		length = sb.getPos()-sp.getPos();
	}
}
