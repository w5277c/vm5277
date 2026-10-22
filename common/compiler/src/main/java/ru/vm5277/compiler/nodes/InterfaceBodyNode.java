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
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.InitNodeHolder;

public class InterfaceBodyNode extends AstNode {
	protected List<AstNode> children = new ArrayList<>();
	
	public InterfaceBodyNode(Instance inst, TokenBuffer tb, String className, ObjectTypeNode ifaceNode) throws CompileException {
		super(inst, tb);

		consumeToken(tb, Delimiter.LEFT_BRACE);

		while (!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_BRACE)) {
			Map<String, AnnotationParamNode> annotations = collectAnnotations(tb);
			Set<Keyword> modifiers = collectModifiers(tb);

			// Обработка вложенных интерфейсов
			if (tb.match(TokenType.OOP, J8BKeyword.INTERFACE)) {
				InterfaceNode iNode = new InterfaceNode(inst, tb, annotations, modifiers, null, true, new ArrayList<>());
				children.add(iNode);
				continue;
			}
			// Обработка exception с модификаторами
			if (tb.match(TokenType.OOP, J8BKeyword.EXCEPTION)) {
				ExceptionNode eNode = new ExceptionNode(inst, tb, modifiers, new ArrayList<>());
				children.add(eNode);
				continue;
			}
			// Обработка enum с модификаторами
			if (tb.match(TokenType.OOP) && J8BKeyword.ENUM == tb.current().getValue()) {
				//markError("Enums are not allowed in interfaces");
    			EnumNode eNode = new EnumNode(inst, tb, annotations, modifiers, ifaceNode);
//				eNode.disable();
				children.add(eNode);
				continue;
			}
			// Обработка вложенных классов
			if (tb.match(TokenType.OOP, J8BKeyword.CLASS)) {
				ClassNode cNode = new ClassNode(inst, tb, annotations, modifiers, true, new ArrayList<>());
				// Проверяем что класс статический
				//TODO Продумать необходимость/возможность наличия статических классов в интерфейсе
				if(!modifiers.contains(J8BKeyword.STATIC)) {
					markError("Inner classes in interfaces must be static");
					cNode.disable();
				}
				children.add(cNode);
				continue;
			}

			// Определение типа (примитив или класс)
			VarType type = checkPrimtiveType();
			if(null == type) type = checkClassType();
			if(null != type) type = checkArrayType(type);

			// Получаем имя поля/метода
			String name = null;
			if (tb.match(TokenType.IDENTIFIER)) {
				name = consumeToken(tb).getStringValue();
			}

			if (tb.match(Delimiter.LEFT_PAREN)) { // Это метод
				children.add(new MethodNode(inst, tb, annotations, modifiers, type, name, ifaceNode));
				continue;
			}

			if (null != type) { // Это поле
				children.add(new FieldNode(inst, tb, ifaceNode, annotations, modifiers, type, name));
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
	public boolean preAnalyze() {
		// Проверка всех объявлений в блоке
		for (AstNode node : children) {
			if(node instanceof InterfaceNode || node instanceof EnumNode) {
				node.preAnalyze();
			}
			else if(node instanceof FieldNode) {
				node.preAnalyze();
			}
			else if(node instanceof MethodNode) {
				node.preAnalyze();
			}
			else {
				markError(	"Interface cannot contain " + node.getClass().getSimpleName() +
							" declarations. Only methods, constants and nested types are allowed");
			}
		}
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		for(AstNode node : children) {
			node.declare(scope);
		}
		return true;
	}

	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		for (AstNode node : children) {
			if (node instanceof InterfaceNode || node instanceof ClassNode) {
				// Обработка вложенных интерфейсов и классов
				result &= node.postAnalyze(scope, cg, parent);
			} 
			else if (node instanceof FieldNode) {
				FieldNode fieldNode = (FieldNode)node;

				result &= fieldNode.postAnalyze(scope, cg, parent);
				
				// Проверка инициализации поля
				if(null!=fieldNode.getInitializer() || !fieldNode.getType().getClassName().equals(VarType.CLASSNAME_FLASH_DATA)) {
					if(null==fieldNode.getInitializer()) {
						markError("Interface field '" + fieldNode.getName() + "' must be initialized");
					}
					else {
						result &= fieldNode.getInitializer().postAnalyze(scope, cg, parent);
					}
					if(result && null!=fieldNode.getInitializer()) {
						// Резолвинг QualifiedPathExpression
						ExpressionNode resolved = resolveQualifiedPathExpr(fieldNode.getInitializer());
						if(null!=resolved) {
							fieldNode.setInitializer(resolved);
						}


						if(fieldNode.getInitializer() instanceof VarFieldExpression) {
							ExpressionNode en = ((InitNodeHolder)((AstHolder)((VarFieldExpression)fieldNode.getInitializer()).getSymbol()).getNode()).getInitNode();
							fieldNode.setInitializer(en);
						}
					}
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
				if(methodNode.isConstructor()) {
					markError("Interfaces cannot have constructors");
					continue;
				}

				// Проверка методов
				if(methodNode.isStatic() && !methodNode.isNative()) {
					// Для статических методов должно быть тело
					if(null==methodNode.getBody()) {
						markError("Static method '" + methodNode.getName() + "' must have a body");
					}
					else {
						result &= methodNode.postAnalyze(scope, cg, parent);
					}
				}
				else {
					// Для нестатических методов не должно быть тела (абстрактные)
					if(null!=methodNode.getBody()) {
						markError("Non-static interface method '" + methodNode.getName() + "' cannot have a body");
					}
				}

				// Проверка что метод public
				if(!methodNode.isPublic()) {
					markError("Interface method '" + methodNode.getName() + "' must be public");
				}
			}
		}

		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
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