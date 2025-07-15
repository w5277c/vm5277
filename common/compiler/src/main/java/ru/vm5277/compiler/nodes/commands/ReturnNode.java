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
package ru.vm5277.compiler.nodes.commands;

import java.util.List;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class ReturnNode extends CommandNode {
	private	ExpressionNode	expression;
	private	VarType			returnType;
	
	public ReturnNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
		consumeToken(tb); // Потребляем "return"
		
		try {this.expression = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
        
        // Обязательно потребляем точку с запятой
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
    }

	public ReturnNode(MessageContainer mc, ExpressionNode expression) {
		super(null, mc);
		
		this.expression = expression;
	}
	
    public ExpressionNode getExpression() {
        return expression;
    }

    public boolean returnsValue() {
        return null != expression;
    }
	
	@Override
	public String getNodeType() {
		return "return command";
	}

	@Override
	public boolean preAnalyze() {
		if (expression != null) {
			if(!expression.preAnalyze()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if(null != expression) {
			if(!expression.declare(scope)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		cgScope = cg.getScope();
	
		// Находим ближайший MethodScope
		MethodScope methodScope = findEnclosingMethodScope(scope);
		if (null == methodScope) {
			markError("'return' outside of method");
		}
		else {
			// Получаем тип возвращаемого значения метода
			returnType = methodScope.getSymbol().getType();

			// Проверяем соответствие возвращаемого значения
			if (null == expression) { // return без значения
				if (!returnType.isVoid()) markError("Void method cannot return a value");
			}
			else { // return с выражением
				if (returnType.isVoid()) markError("Non-void method must return a value");
				else {
					if(!expression.postAnalyze(scope, cg)) {
						return false;
					}
					// Проверяем тип выражения
					try {
						VarType exprType = expression.getType(scope);
						if(null == exprType) {
							markError(String.format("TODO Can't resolve expression: " + expression));
						}
						else {
							if (!isCompatibleWith(scope, returnType, exprType)) {
								markError(String.format("Return type mismatch: expected " + returnType + ", got " + exprType));
							}
						}
					}
					catch (SemanticException e) {markError(e);}
				}
			}
		}
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		if(null != expression) {
			expression.codeGen(cg);
		}
		cg.eReturn(cgScope, returnType.getSize());
		
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}
