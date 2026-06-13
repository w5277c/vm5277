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

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;

public class J8BLanguageServer extends LanguageServer {
	private	final	Lexer	lexer	= new Lexer(LexerType.J8B);

	@Override
	public LSPToken readNextToken(SourceBuffer sb) {
		Token token = lexer.parseToken(sb);
		if(null==token || TokenType.EOF==token.getType()) {
			return null;
		}
		
		if(TokenType.LABEL==token.getType()) {
			labels.add(token.getRaw());
		}

		return new LSPToken(token);
	}
	
	@Override
	public List<Token> tokenize(SourceBuffer sb) {
		List<Token> result = new ArrayList<>();
		while(sb.available()) {
			result.add(lexer.parseToken(sb));
		}
		return result;
	}

	public static List<String> getKeywords() {
		List<String> result = new ArrayList<>();
		for(Keyword keyword : Keyword.getItems()) {
			if(LexerType.ALL==keyword.getLexerType() || LexerType.J8B==keyword.getLexerType()) {
				result.add(keyword.getName());
			}
		}
		return result;
	}
}

