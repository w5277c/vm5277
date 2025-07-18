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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;

public class InterfaceBodyNode extends AstNode {
	protected List<AstNode> children = new ArrayList<>();
	
	public InterfaceBodyNode(TokenBuffer tb, MessageContainer mc, String className) throws CompileException {
		super(tb, mc);

		consumeToken(tb, Delimiter.LEFT_BRACE);

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка вложенных интерфейсов
			if (tb.match(TokenType.OOP, Keyword.INTERFACE)) {
				children.add(new InterfaceNode(tb, mc, modifiers, null));
				continue;
			}

			// Обработка вложенных классов
			if (tb.match(TokenType.OOP, Keyword.CLASS)) {
				children.add(new ClassNode(tb, mc, modifiers, null, null));
				continue;
			}

			// Определение типа (примитив или класс)
			VarType type = checkPrimtiveType();
			if (null == type) type = checkClassType();

			// Получаем имя поля/метода
			String name = null;
			if (tb.match(TokenType.ID)) {
				name = consumeToken(tb).getStringValue();
			}

			if (tb.match(Delimiter.LEFT_PAREN)) { // Это метод
				children.add(new MethodNode(tb, mc, modifiers, type, name));
				continue;
			}

			if (null != type) { // Это поле
				children.add(new FieldNode(tb, mc, modifiers, type, name));
				continue;
			}

			// Если ничего не распознано, пропускаем токен
			markError("Unexpected token in interface body: " + tb.current());
			tb.consume();
		}

		try {
			consumeToken(tb, Delimiter.RIGHT_BRACE);
		} catch (CompileException e) {
			markFirstError(e);
		}
	}
	
	public List<AstNode> getDeclarations() {
		return children;
	}
	
	@Override
	public String getNodeType() {
		return "interface body";
	}
	
	@Override
	public boolean preAnalyze() {
		// Проверка всех объявлений в блоке
		for (AstNode node : children) {
			if(node instanceof InterfaceNode) {
				node.preAnalyze();
			}
			else if(node instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)node;
				if(!fieldNode.getModifiers().isEmpty()) {
					//TODO не сохраняется позиция для конкретного modifier
					addMessage(new WarningMessage("Modifiers not allowed for interface fields (already public static final)", fieldNode.getSP()));
				}
				node.preAnalyze();
			}
			else if(node instanceof MethodNode) {
				MethodNode methoddNode = (MethodNode)node;
				if(!methoddNode.getModifiers().isEmpty()) {
					//TODO не сохраняется позиция для конкретного modifier
					addMessage(new WarningMessage("Modifiers not allowed for interface methods (already public abstract)", methoddNode.getSP()));
				}
				node.preAnalyze();
			}
			else {
				markError("Interface cannot contain " + node.getNodeType() + " declarations. Only methods, constants and nested types are allowed");
			}
		}
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		for (AstNode node : children) {
			node.declare(scope);
		}
		return true;
	}

	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		ClassScope classScope = (ClassScope)scope;

		for (AstNode node : children) {
			if (node instanceof InterfaceNode || node instanceof ClassNode) {
				// Обработка вложенных интерфейсов и классов
				node.postAnalyze(scope, cg);
			} 
			else if (node instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)node;

				// Проверка инициализации поля
				if (null == fieldNode.getInitializer()) {
					markError("Interface field '" + fieldNode.getName() + "' must be initialized");
				} else {
					fieldNode.getInitializer().postAnalyze(scope, cg);
				}

				// Проверка что поле static final
				if (!fieldNode.isStatic()) {
					markError("Interface field '" + fieldNode.getName() + "' must be static");
				}
				if (!fieldNode.isFinal()) {
					markError("Interface field '" + fieldNode.getName() + "' must be final");
				}
			} 
			else if (node instanceof MethodNode) {
				MethodNode methodNode = (MethodNode)node;

				// Запрет конструкторов
				if (methodNode.isConstructor()) {
					markError("Interfaces cannot have constructors");
					continue;
				}

				// Проверка методов
				if (methodNode.isStatic()) {
					// Для статических методов должно быть тело
					if (null == methodNode.getBody()) {
						markError("Static method '" + methodNode.getName() + "' must have a body");
					} else {
						methodNode.getBody().postAnalyze(scope, cg);
					}
				} else {
					// Для нестатических методов не должно быть тела (абстрактные)
					if (null != methodNode.getBody()) {
						markError("Non-static interface method '" + methodNode.getName() + "' cannot have a body");
					}
				}

				// Проверка что метод public
				if (!methodNode.isPublic()) {
					markError("Interface method '" + methodNode.getName() + "' must be public");
				}
			}
		}

		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;

/*		for(AstNode node : children) {
			if(node instanceof MethodNode && ((MethodNode)node).isStatic()) {
				node.codeGen(cg);
			}
			else if(node instanceof FieldNode && ((FieldNode)node).isStatic()) {
				node.codeGen(cg);
			}
		}
*/		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return children;
	}
}