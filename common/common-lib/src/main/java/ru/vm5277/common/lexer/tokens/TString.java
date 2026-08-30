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

public class TString extends Token {
	public TString(SourceBuffer sb) throws CompileException {
		super(sb);
		type = TokenType.STRING;
		
		StringBuilder str = new StringBuilder();
		sb.next(); // Пропускаем '"'
		while(sb.available() && '"'!=sb.peek()) {
			str.append(sb.peek());
			sb.next();			
        }
		if(!sb.available()) {
			throw new CompileException("Unterminated string literal");
		}
		else {
			sb.next();
		}
        value = str.toString();
		
		length = sb.getPos()-sp.getPos();
	}
}
