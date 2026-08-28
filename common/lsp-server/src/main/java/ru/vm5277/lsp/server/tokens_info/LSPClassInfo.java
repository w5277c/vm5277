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

import java.util.List;
import ru.vm5277.common.lexer.TokenType;

public class LSPClassInfo extends LSPNodeInfoHasMethods {
	private List<LSPFieldInfo> fields;
	private List<LSPNodeInfo> internals;

	public LSPClassInfo(String fullName) {
		super(TokenType.IDENTIFIER);
		
		this.fullName = fullName;
		int lastDot = fullName.lastIndexOf('.');
		this.name = lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
		this.packageName = lastDot > 0 ? fullName.substring(0, lastDot) : "";
		this.kind = "class";
	}

	public List<LSPFieldInfo> getFields() { return fields; }
	public void setFields(List<LSPFieldInfo> fields) { this.fields = fields; }
	public List<LSPNodeInfo> getInternals() { return internals; }
	public void setInternals(List<LSPNodeInfo> internals) { this.internals = internals; }
}