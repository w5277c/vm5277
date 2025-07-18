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
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class FieldNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			VarType			type;
	private			String			name;
	private			ExpressionNode	initializer;
	
	public FieldNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType  type, String name) {
		super(tb, mc);

		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            initializer = null;
        }
		else {
			consumeToken(tb);
			try {initializer = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return initializer;
	}
	
	public boolean isStatic() {
		return modifiers.contains(Keyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(Keyword.PUBLIC);
	}
	public boolean isPrivate() {
		return modifiers.contains(Keyword.PRIVATE);
	}

	@Override
	public String getNodeType() {
		return "field";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + type + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		if((!isFinal() || !isStatic()) && Character.isUpperCase(name.charAt(0))) {
			addMessage(new WarningMessage("Field name should start with lowercase letter:" + name, sp));
		}

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if(scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;
			boolean isFinal = modifiers.contains(Keyword.FINAL);
			symbol = new FieldSymbol(name, type, isFinal, isStatic(), isPrivate(), classScope, this);

			//TODO рудимент
			/*
			if(isFinal && null != initializer) {
				if(initializer instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)initializer;
					symbol.setOperand(new Operand(le.getType(scope), OperandType.LITERAL, le.getValue()));
				}
				else if(initializer instanceof VarFieldExpression) {
					VarFieldExpression ve = (VarFieldExpression)initializer;
					symbol.setOperand(new Operand(ve.getType(scope), OperandType.TYPE, ve.getValue()));
				}
			}*/
			
			try{classScope.addField(symbol);}
			catch(CompileException e) {markError(e);}
		}
		else markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in filed:" + name);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			symbol.setCGScope(cg.enterField(type, isStatic(), name));

			// Проверка инициализации final-полей
			if (isFinal() && initializer == null) markError("Final field '" + name + "' must be initialized");

			// Анализ инициализатора, если есть
			if (initializer != null) initializer.postAnalyze(scope, cg);

			// Проверка совместимости типов
			VarType initType = initializer.getType(scope);
			if (!isCompatibleWith(scope, type, initType)) {
				markError("Type mismatch: cannot assign " + initType + " to " + type);
			}
			// Дополнительная проверка на сужающее преобразование
			if (type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) {
				markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast");
			}
			
			ExpressionNode optimizedExpr = initializer.optimizeWithScope(scope);
			if(null != optimizedExpr) {
				initializer = optimizedExpr;
			}
		}
		catch (CompileException e) {markError(e);}

		cg.leaveField();		
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;

		cgDone = true;
		((CGFieldScope)symbol.getCGScope()).build();
		initializer.codeGen(cg);
		cg.accToCells(symbol.getCGScope(), (CGFieldScope)symbol.getCGScope());

		return true;
	}

	@Override
	public List<AstNode> getChildren() {
		if(null != initializer) return Arrays.asList(initializer);
		return null;
	}
}
