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
import ru.vm5277.common.compiler.CodeGenerator;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class InterfaceNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private			String				name;
	private			String				parentClassName;
	private			List<String>		interfaces		= new ArrayList<>();
	private			InterfaceBodyNode	blockNode;

	public InterfaceNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, String parentClassName) throws ParseException {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск interface токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();
			VarType.addClassName(this.name);
		}
		catch(ParseException e) {markFirstError(e);} // ошибка в имени, оставляем null
		
        // Парсинг интерфейсов (если есть)
		if (tb.match(Keyword.IMPLEMENTS)) {
			consumeToken(tb);
			while(true) {
				try {
					interfaces.add((String)consumeToken(tb, TokenType.ID).getValue());
				}
				catch(ParseException e) {markFirstError(e);} // встретили не ID интерфейса, пропускаем
				if (!tb.match(Delimiter.COMMA)) break;
				consumeToken(tb);
			}
		}
        // Парсинг тела интерфейса
		blockNode = new InterfaceBodyNode(tb, mc, name);
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

	public InterfaceBodyNode getBody() {
		return blockNode;
	}
	
	@Override
	public String getNodeType() {
		return "interface";
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + name + ", " + interfaces;
	}

	@Override
	public boolean preAnalyze() {
		try {validateName(name);} catch(SemanticException e) {addMessage(e);	return false;}

		if(Character.isLowerCase(name.charAt(0))) {
			addMessage(new WarningMessage("Interface name should start with uppercase letter:" + name, sp));
		}
		
		try{validateModifiers(modifiers, Keyword.PUBLIC);} catch(SemanticException e) {addMessage(e);}
		
		// Анализ тела интерфейса
		blockNode.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		try {
			InterfaceSymbol interfaceSymbol = new InterfaceSymbol(name);
			if (scope instanceof ClassScope) {
				((ClassScope)scope).addInterface(interfaceSymbol);
			}
			blockNode.declare(scope);
		} 
		catch (SemanticException e) {
			markError(e);
		}
		return true;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка что интерфейс не содержит конструкторов
		for (AstNode decl : blockNode.getDeclarations()) {
			if (decl instanceof MethodNode) {
				MethodNode method = (MethodNode)decl;
				if (method.isConstructor()) {
					markError("Interfaces cannot contain constructors");
					break;
				}
			}
		}

		
		for (AstNode decl : blockNode.getDeclarations()) {
			if (decl instanceof FieldNode) { // Проверка что все поля - public static final и инициализированы
				FieldNode field = (FieldNode)decl;

				if (!field.getModifiers().contains(Keyword.PUBLIC)) markError("Interface field '" + field.getName() + "' must be public");
				if (!field.getModifiers().contains(Keyword.STATIC)) markError("Interface field '" + field.getName() + "' must be static");
				if (!field.getModifiers().contains(Keyword.FINAL)) markError("Interface field '" + field.getName() + "' must be final");

				if (null == field.getInitializer()) markError("Interface field '" + field.getName() + "' must be initialized");
			}
			else if (decl instanceof MethodNode) { // Проверка что методы не имеют реализации
				MethodNode method = (MethodNode)decl;
				if (null != method.getBody()) {
					markError("Interface method '" + method.getName() + "' cannot have a body");
				}
			}
		}

		// Проверка вложенных интерфейсов
		for (AstNode decl : blockNode.getDeclarations()) {
			if (decl instanceof InterfaceNode) {
				decl.postAnalyze(scope);
			}
		}

		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		int[] interfaceIds = null;
		if(!interfaces.isEmpty()) {
			interfaceIds = new int[interfaces.size()];
			for(int i=0; i<interfaces.size(); i++) {
				interfaceIds[i] = VarType.fromClassName(interfaces.get(i)).getId();
			}
		}
			
		cg.enterClass(VarType.fromClassName(name).getId(), interfaceIds);
		try {
			blockNode.codeGen(cg);
		}
		finally {
			cg.leave();
		}
	}
}

