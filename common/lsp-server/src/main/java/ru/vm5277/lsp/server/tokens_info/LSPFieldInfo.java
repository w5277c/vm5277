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

public class LSPFieldInfo extends LSPNodeInfo {
	private String varType;
	private boolean isStatic;
	private boolean isPublic;

	public LSPFieldInfo(String name, String varType) {
		super(name, TokenType.IDENTIFIER, "field", 0);
	}

	public String getVarType() { return varType; }
	
	public boolean isStatic() { return isStatic; }
	public void setStatic(boolean isStatic) { this.isStatic = isStatic; }

	public boolean isPublic() { return isPublic; }
	public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
}