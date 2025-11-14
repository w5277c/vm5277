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

package ru.vm5277.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;

public abstract class ObjectTypeNode extends AstNode {
	protected			List<ObjectTypeNode>	importedClasses;
	protected			Set<Keyword>			modifiers;
	protected			String					name;
	protected			String					parentClassName;
	protected			List<String>			impl			= new ArrayList<>();
	
	public ObjectTypeNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, String parentClassName, List<ObjectTypeNode> importedClasses)
																																	throws CompileException {
		super(tb, mc);
		
		this.importedClasses = importedClasses;
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск class токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();
			VarType.addClassName(this.name, false);
		}
		catch(CompileException e) {markFirstError(e);} // ошибка в имени, оставляем null
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return null == parentClassName ? name : parentClassName + "." + name;
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	public abstract AstNode getBody();
}
