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

package ru.vm5277.lsp.server;

import ru.vm5277.common.lexer.tokens.Token;

public class LSPToken {
	private	String	type;
	private	int		startLine;
	private	int		startColumn;
	private	int		length;

	public LSPToken(String type, int startLine, int startColumn, int length) {
		this.type = type;
		this.startLine = startLine;
		this.startColumn = startColumn;
	}

	public LSPToken(Token token) {
		this.type = token.getType().name();
		this.startLine = token.getSP().getLine();
		this.startColumn = token.getSP().getColumn();
		this.length = token.getLength();
	}
	
	public String getType() {
		return type;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}
	
	public int getLength() {
		return length;
	}
	
	@Override
	public String toString() {
		return type + ", " + startLine + ":" + startColumn + ", " + length;
	}
}
