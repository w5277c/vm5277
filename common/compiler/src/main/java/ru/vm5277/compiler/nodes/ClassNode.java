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
import java.util.Map;
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
import ru.vm5277.compiler.semantic.MethodSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class ClassNode extends AstNode {
	private	final	List<ClassNode>	importedClasses;
	private	final	Set<Keyword>	modifiers;
	private			String			name;
	private			String			parentClassName;
	private			List<String>	interfaces	= new ArrayList<>();
	private			ClassBlockNode	blockNode;
	private			ClassScope		classScope;
	
	public ClassNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, String parentClassName, List<ClassNode> importedClasses)
																																		throws ParseException {
		super(tb, mc);
		
		this.importedClasses = importedClasses;
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск class токена
		try {
			this.name = (String)consumeToken(tb, TokenType.ID).getValue();
			VarType.addClassName(this.name);
		}
		catch(ParseException e) {markFirstError(e);} // ошибка в имени, оставляем null
		
        // Парсинг интерфейсов (если есть)
		if (tb.match(TokenType.OOP, Keyword.IMPLEMENTS)) {
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
        // Парсинг тела класса
		blockNode = new ClassBlockNode(tb, mc, name);
	}
	
	public ClassNode(MessageContainer mc, Set<Keyword> modifiers, String parentClassName, List<String> interfaces) throws ParseException {
		super(null, mc);
		
		this.importedClasses = null;
		this.modifiers = modifiers;
		this.name = parentClassName;
		this.interfaces = interfaces;
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return null == parentClassName ? name : parentClassName + "." + name;
	}
	
	public ClassBlockNode getBody() {
		return blockNode;
	}
	public void setBody(ClassBlockNode body) {
		blockNode = body;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "class";
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + name + ", " + interfaces;
	}

	@Override
	public boolean preAnalyze() {
		try {validateName(name);} catch(SemanticException e) {addMessage(e);	return false;}

		if(Character.isLowerCase(name.charAt(0))) {
			addMessage(new WarningMessage("Class name should start with uppercase letter:" + name, sp));
		}
		
		try{validateModifiers(modifiers, Keyword.PUBLIC, Keyword.PRIVATE, Keyword.STATIC);} catch(SemanticException e) {addMessage(e);}
		
		// Анализ тела класса
		blockNode.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope parentScope) {
		if(null != importedClasses) {
			for (ClassNode imported : importedClasses) {
				imported.declare(parentScope);
			}
		}
		
		try {
			classScope = new ClassScope(name, parentScope);
			if(null != parentScope) ((ClassScope)parentScope).addClass(classScope);
			
			blockNode.declare(classScope);
		}
		catch(SemanticException e) {markError(e); return false;}

		return true;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope) {
		for (String interfaceName : interfaces) {
			// Проверяем существование интерфейса
			InterfaceSymbol interfaceSymbol = classScope.resolveInterface(interfaceName);
			if (null == interfaceSymbol) markError("Interface not found: " + interfaceName);

			checkInterfaceImplementation(classScope, interfaceSymbol);
		}
		
		blockNode.postAnalyze(classScope);

		return true;
	}
	
	
	private boolean checkInterfaceImplementation(ClassScope classScope, InterfaceSymbol interfaceSymbol) {
		boolean allMethodsImplemented = true;
		
		Map<String, List<MethodSymbol>> map = interfaceSymbol.getMethods();
		for(String methodName : map.keySet()) { 
			List<MethodSymbol> entry = map.get(methodName);
			boolean found = false;

			// Получаем методы класса с таким же именем
			List<MethodSymbol> classMethods = classScope.getMethods(methodName);

			// Для каждого метода в интерфейсе
			for (MethodSymbol interfaceMethod : entry) {

				// Проверяем каждый метод класса
				for (MethodSymbol classMethod : classMethods) {
					if (interfaceMethod.getSignature().equals(classMethod.getSignature())) {
						found = true;
						break;
					}
				}

				if (!found) {
					markError(	"Class '" + classScope.getName() + "' must implement method: " + interfaceMethod.getSignature() + 
								" from interface '" + interfaceSymbol.getName() + "'");
					allMethodsImplemented = false;
				}
			}
		}
		return allMethodsImplemented;
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
			
		cg.enterClass(VarType.fromClassName(name).getId(), interfaceIds, name);
		try {
			blockNode.codeGen(cg);
		}
		finally {
			cg.leaveClass();
		}
	}
}
