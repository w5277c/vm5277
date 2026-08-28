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

package ru.vm5277.lsp.server.tokens_info;

import ru.vm5277.common.lexer.TokenType;

public class LSPNodeInfo {
	protected	String		name;
	protected	String		fullName;
	protected	String		packageName;
	private		String		fileUri;

	protected	TokenType	type;
	protected	String		kind;
	protected	int			priority = 0;

	public LSPNodeInfo(TokenType type) {
		this.type = type;
	}
	public LSPNodeInfo(String name, TokenType type, String kind, int priority) {
		this.name = name;
		this.type = type;
		this.kind = kind;
		this.priority = priority;
	}

	public String getFullName() { return fullName; }
	public void setFullName(String fullName) { this.fullName = fullName; }

	public String getPackageName() { return packageName; }
	public void setPackageName(String packageName) { this.packageName = packageName; }

	public String getFileUri() { return fileUri; }
	public void setFileUri(String fileUri) { this.fileUri = fileUri; }

	public String getName() {
		return name;
	}
	public void setName(String name)  {
		this.name = name;
	}
	
	public TokenType getType() {
		return type;
	}
	
	public String getKind() {
		return kind;
	}
	public void setKind(String kind) {
		this.kind = kind;
	}
	
	public int getPriority() {
		return priority;
	}
	public void setPriority(int priority) {
		this.priority = priority;
	}
}