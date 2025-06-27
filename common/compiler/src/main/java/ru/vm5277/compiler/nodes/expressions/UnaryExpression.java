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
package ru.vm5277.compiler.nodes.expressions;

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.Operator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class UnaryExpression extends ExpressionNode {
    private final Operator		 operator;
    private final ExpressionNode operand;
    
    public UnaryExpression(TokenBuffer tb, MessageContainer mc, Operator operator, ExpressionNode operand) {
        super(tb, mc);
        
		this.operator = operator;
        this.operand = operand;
    }
    
	@Override
	public VarType getType(Scope scope) throws SemanticException {
		VarType operandType = operand.getType(scope);

		switch (operator) {
			case NOT: return VarType.BOOL;
			case PRE_INC:
			case PRE_DEC:
			case POST_INC:
			case POST_DEC:
			case PLUS:
			case MINUS: return VarType.FIXED;
			case BIT_NOT:
			default: return operandType;
		}
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			// Проверяем операнд
			if (!operand.postAnalyze(scope)) {
				return false;
			}

			VarType operandType = operand.getType(scope);

			// Проверяем допустимость оператора для типа
			switch (operator) {
				case PLUS:
					if (!operandType.isNumeric() && VarType.CSTR != operandType) {
						markError("Unary " + operator + " requires numeric or string type");
						return false;
					}
					break;
					
				case MINUS:
					if (!operandType.isNumeric()) {
						markError("Unary " + operator + " requires numeric type");
						return false;
					}
					break;

				case BIT_NOT:
					if (!operandType.isInteger()) {
						markError("Bitwise ~ requires integer type");
						return false;
					}
					break;

				case NOT:
					if (operandType != VarType.BOOL) {
						markError("Logical ! requires boolean type");
						return false;
					}
					break;

				case PRE_INC:
				case PRE_DEC:
				case POST_INC:
				case POST_DEC:
					if (!operandType.isNumeric()) {
						markError("Increment/decrement requires numeric type");
						return false;
					}
					if (!(operand instanceof VariableExpression)) {
						markError("Can only increment/decrement variables");
						return false;
					}
					// Дополнительная проверка на изменяемость переменной
					if (isFinalVariable((VariableExpression)operand, scope)) {
						markError("Cannot modify final variable");
						return false;
					}
					break;

				default:
					markError("Unsupported unary operator: " + operator);
					return false;
			}

			return true;
		} catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}
	
	private boolean isFinalVariable(VariableExpression var, Scope scope) {
		Symbol symbol = scope.resolve(var.getValue());
		if(null == symbol) {
			ClassScope classScope = scope.getThis().resolveClass(var.getValue());
			if(null != classScope) {
				symbol = new Symbol(var.getValue(), VarType.CLASS, false, false);
			}
		}
		return symbol != null && symbol.isFinal();
	}
	
	
	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getOperand() {
		return operand;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		// Генерация кода для операнда (например, переменной или другого выражения)
		operand.codeGen(cg);

		if(operand instanceof VariableExpression) {
			cg.emitUnary(operator, ((VariableExpression)operand).getSymbol().getRuntimeId()); //Работаем с переменной
		}
		else {
			cg.emitUnary(operator, null); //TODO Работаем с уже загруженным Accum
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + operator + " " + operand;
	}
}