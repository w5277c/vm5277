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

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class ReturnNode extends CommandNode {
	private	ExpressionNode	expression;
	
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
		if (expression != null) expression.preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Находим ближайший MethodScope
		MethodScope methodScope = findEnclosingMethodScope(scope);
		if (null == methodScope) {
			markError("'return' outside of method");
		}
		else {
			// Получаем тип возвращаемого значения метода
			VarType methodReturnType = methodScope.getSymbol().getType();

			// Проверяем соответствие возвращаемого значения
			if (null == expression) { // return без значения
				if (!methodReturnType.isVoid()) markError("Void method cannot return a value");
			}
			else { // return с выражением
				if (methodReturnType.isVoid()) markError("Non-void method must return a value");
				else {
					// Проверяем тип выражения
					try {
						VarType exprType = expression.getType(scope);
						if (!isCompatibleWith(scope, exprType, methodReturnType)) {
							markError(String.format("Return type mismatch: expected " + methodReturnType + ", got " + exprType));
						}
						
						// Дополнительная проверка на сужающее преобразование
						if (exprType.isNumeric() && methodReturnType.isNumeric() && exprType.getSize() < methodReturnType.getSize()) {
							markError("Narrowing conversion from " + methodReturnType + " to " + exprType + " requires explicit cast");
						}

					}
					catch (SemanticException e) {markError(e);}
				}
			}
		}
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		if(null != expression) {
			expression.codeGen(cg);
		}
		cg.eReturn();
	}
}
