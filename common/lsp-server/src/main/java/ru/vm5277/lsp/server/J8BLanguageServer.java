/*
 * Copyright 2026 konstantin@5277.ru
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

package ru.vm5277.lsp.server;

import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;

public class J8BLanguageServer implements LanguageServer {
	private	final	Lexer	lexer	= new Lexer(LexerType.J8B);

	@Override
	public LSPToken readNextToken(SourceBuffer sb) {
		Token token = lexer.parseToken(sb);
		if(null==token || TokenType.EOF==token.getType()) {
			return null;
		}
		return new LSPToken(token);
	}
}

