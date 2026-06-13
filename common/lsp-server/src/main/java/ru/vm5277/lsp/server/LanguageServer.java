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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.tokens.Token;

public abstract class LanguageServer {
	protected	Set<String>	labels	= new HashSet<>();
	
	public Set<String> getLabels() {
		return labels;
	}
	
	public abstract LSPToken readNextToken(SourceBuffer sb);
	public abstract List<Token> tokenize(SourceBuffer sb);
}
