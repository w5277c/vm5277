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

import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.LexerType;

public class TKeyword extends Token {
	public TKeyword(SourceBuffer sb, LexerType lexerType) {
		super(sb);
		
		StringBuilder stringBuilder = new StringBuilder();
        while (sb.available() && (Character.isLetterOrDigit(sb.peek()) || '_'==sb.peek())) {
            stringBuilder.append(sb.peek());
			sb.next();
        }
        String id = (LexerType.ASM==lexerType ? stringBuilder.toString().toLowerCase() : stringBuilder.toString());
		Keyword keyword = Keyword.fromString(id, lexerType);
		
		if(null!=keyword) {
			type = keyword.getTokenType();
			value = keyword;
		}
		else {
			type = TokenType.IDENTIFIER;
			value = id;
		}
		length = sb.getPos()-sp.getPos();
	}
}
