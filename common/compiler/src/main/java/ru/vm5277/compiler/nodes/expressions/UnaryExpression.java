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

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGConditionScope;
import ru.vm5277.common.cg.scopes.CGExpressionScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class UnaryExpression extends ExpressionNode {
    private	final	Operator		operator;
    private	final	ExpressionNode	operand;
    private			boolean			isUsed		= false;
	
    public UnaryExpression(TokenBuffer tb, MessageContainer mc, Operator operator, ExpressionNode operand) {
        super(tb, mc);
        
		this.operator = operator;
        this.operand = operand;
    }
    
	@Override
	public VarType getType(Scope scope) throws CompileException {
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			cgScope = cg.enterExpression();
			
			// Проверяем операнд
			if (!operand.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}

			VarType operandType = operand.getType(scope);

			// Проверяем допустимость оператора для типа
			switch (operator) {
				case PLUS:
					if (!operandType.isNumeric() && VarType.CSTR != operandType) {
						markError("Unary " + operator + " requires numeric or string type");
						cg.leaveExpression();
						return false;
					}
					break;
					
				case MINUS:
					if (!operandType.isNumeric()) {
						markError("Unary " + operator + " requires numeric type");
						cg.leaveExpression();
						return false;
					}
					break;

				case BIT_NOT:
					if (!operandType.isInteger()) {
						markError("Bitwise ~ requires integer type");
						cg.leaveExpression();
						return false;
					}
					break;

				case NOT:
					if (operandType != VarType.BOOL) {
						markError("Logical ! requires boolean type");
						cg.leaveExpression();
						return false;
					}
					break;

				case PRE_INC:
				case PRE_DEC:
				case POST_INC:
				case POST_DEC:
					if (!operandType.isNumeric()) {
						markError("Increment/decrement requires numeric type");
						cg.leaveExpression();
						return false;
					}
					if (!(operand instanceof VarFieldExpression)) {
						markError("Can only increment/decrement variables");
						cg.leaveExpression();
						return false;
					}
					// Дополнительная проверка на изменяемость переменной
					if (isFinalVariable((VarFieldExpression)operand, scope)) {
						markError("Cannot modify final variable");
						cg.leaveExpression();
						return false;
					}
					break;

				default:
					markError("Unsupported unary operator: " + operator);
					cg.leaveExpression();
					return false;
			}
			
			cg.leaveExpression();
			return true;
		}
		catch (CompileException e) {
			markError(e.getMessage());
			cg.leaveExpression();
			return false;
		}
	}
	
	private boolean isFinalVariable(VarFieldExpression var, Scope scope) {
		Symbol symbol = scope.resolveSymbol(var.getValue());
		if(null == symbol) {
			InterfaceScope iScope = scope.getThis().resolveScope(var.getValue());
			if(null != iScope) {
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
	public Object codeGen(CodeGenerator cg, boolean accumStore) throws Exception {
		return codeGen(cg, false, accumStore);
	}
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean accumStore) throws Exception {

		// Генерация кода для операнда (например, переменной или другого выражения)

		if(Operator.NOT == operator) {
			if(operand instanceof BinaryExpression) {
				((BinaryExpression)operand).codeGen(cg, !isInvert, true, false);
			}
			else if(operand instanceof UnaryExpression) {
				((UnaryExpression)operand).codeGen(cg, !isInvert, false);
			}
			else {
				throw new CompileException("Unsupported expression: '" + operand + " for operator:" + operator);
			}
		}
		else {
			if(operand instanceof VarFieldExpression) {
				CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope();
				cg.emitUnary(cgScope, operator, 0, cScope.getCells()); //TODO смещение!
				//cg.emitUnary(operator, operand.getSymbol().getCGScope().getResId()); //Работаем с переменной
			}
			else if(operand instanceof UnaryExpression) {
				((UnaryExpression)operand).codeGen(cg, isInvert, false);
			}
			else {
				throw new CompileException("Unsupported operand '" + operand + " in unary expression");
	//			cg.emitUnary(operator, null); //TODO Работаем с уже загруженным Accum
			}
		}
		return null;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(operand);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + operator + " " + operand;
	}
}