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

public class LSPNodeInfoHasMethods extends LSPNodeInfo {
	protected	List<LSPMethodInfo>	methods;
	
	public LSPNodeInfoHasMethods(TokenType type) {
		super(type);
	}
	
	public List<LSPMethodInfo> getMethods() { return methods; }
	public void setMethods(List<LSPMethodInfo> methods) { this.methods = methods; }
}