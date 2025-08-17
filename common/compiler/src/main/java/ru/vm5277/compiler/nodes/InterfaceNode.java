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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;

public class InterfaceNode extends ClassNode {
	private			InterfaceBodyNode	blockIfaceNode;

	public InterfaceNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, String parentClassName, List<ClassNode> importedClasses)
																																	throws CompileException {
		super(tb, mc, modifiers, parentClassName, importedClasses);
	}

	@Override
	public void parse() throws CompileException {
		// Парсинг тела интерфейса
		blockIfaceNode = new InterfaceBodyNode(tb, mc, name, this);
		int t=454;
	}
	
	public InterfaceBodyNode getIfaceBody() {
		return blockIfaceNode;
	}
	
	@Override
	public String getNodeType() {
		return "interface";
	}
	
	@Override
	public boolean preAnalyze() {
		try {validateName(name);} catch(CompileException e) {addMessage(e);	return false;}

		if(Character.isLowerCase(name.charAt(0))) {
			addMessage(new WarningMessage("Interface name should start with uppercase letter:" + name, sp));
		}
		
		try{validateModifiers(modifiers, Keyword.PUBLIC);} catch(CompileException e) {addMessage(e);}
		
		// Анализ тела интерфейса
		blockIfaceNode.preAnalyze();

		return true;
	}
	
	@Override
	public boolean declare(Scope scope) {
		try {
			InterfaceScope iScope = new InterfaceScope(name, scope);
			if (scope instanceof ClassScope) {
				((ClassScope)scope).addInterface(iScope);
			}
			else if (scope instanceof InterfaceScope) {
				((InterfaceScope)scope).addInterface(iScope);
			}

			blockIfaceNode.declare(iScope);
		} 
		catch (CompileException e) {
			markError(e);
		}
		return true;
	}
	
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		int[] interfaceIds = null;
		if(!interfaces.isEmpty()) {
			interfaceIds = new int[interfaces.size()];
			for(int i=0; i<interfaces.size(); i++) {
				interfaceIds[i] = VarType.fromClassName(interfaces.get(i)).getId();
			}
		}
		cg.enterInterface(VarType.fromClassName(name), interfaceIds, name);

		// Проверка что интерфейс не содержит конструкторов
		for (AstNode decl : blockIfaceNode.getDeclarations()) {
			if (decl instanceof MethodNode) {
				MethodNode method = (MethodNode)decl;
				if (method.isConstructor()) {
					markError("Interfaces cannot contain constructors");
					break;
				}
			}
		}

		
		for (AstNode decl : blockIfaceNode.getDeclarations()) {
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
		for (AstNode decl : blockIfaceNode.getDeclarations()) {
			if (decl instanceof InterfaceNode) {
				decl.postAnalyze(scope, cg);
			}
		}

		cg.leaveInterface();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		int[] interfaceIds = null;
		if(!interfaces.isEmpty()) {
			interfaceIds = new int[interfaces.size()];
			for(int i=0; i<interfaces.size(); i++) {
				interfaceIds[i] = VarType.fromClassName(interfaces.get(i)).getId();
			}
		}
			
		cg.enterInterface(VarType.fromClassName(name), interfaceIds, name);
		try {
			blockIfaceNode.codeGen(cg);
		}
		finally {
			cg.leaveInterface();
		}
		
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(blockIfaceNode);
	}
}

