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
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.LexerType;

public class TDelimiter extends Token {
	public TDelimiter(SourceBuffer sb, SourcePosition sp, Delimiter value) {
		super(sb, sp);
		
		this.type = TokenType.DELIMITER;
		this.value = value;
		this.length = sb.getPos()-sp.getPos();
	}

	public static TDelimiter parse(SourceBuffer sb, LexerType lexerType) {
		SourcePosition sp = sb.snapSP();
		Delimiter delimiter = Delimiter.matchDelimiter(sb, lexerType);
		if (null!=delimiter) {
			return new TDelimiter(sb, sp, delimiter);
		}
	    return null;
	}
}
