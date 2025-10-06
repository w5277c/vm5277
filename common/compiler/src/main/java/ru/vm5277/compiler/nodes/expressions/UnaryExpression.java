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
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class UnaryExpression extends ExpressionNode {
    private	final	Operator		operator;
    private			ExpressionNode	operand;
    private			boolean			isUsed		= false;
	
    public UnaryExpression(TokenBuffer tb, MessageContainer mc, Operator operator, ExpressionNode operand) {
        super(tb, mc);
        
		this.operator = operator;
        this.operand = operand;
    }
    
	@Override
	public VarType getType(Scope scope) throws CompileException {
		return operand.getType(scope);
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		
		result &= operand.preAnalyze();
		
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		result&=operand.declare(scope);
		
		if((operator.isPostfix() || Operator.PRE_DEC == operator || Operator.PRE_INC == operator) && operand instanceof VarFieldExpression) {
			VarFieldExpression vfe = (VarFieldExpression)operand;
			symbol = scope.resolveSymbol(vfe.getValue());
			if(null != symbol) {
				symbol.setReassigned();
			}
		}
		return result;
	}


	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());
		//if(null != symbol) symbol.setCGScope(cgScope);
		
		try {
			result&=operand.postAnalyze(scope, cg);

			ExpressionNode newOperand = operand.optimizeWithScope(scope, cg);
			if(null != newOperand) {
				operand = newOperand;
				newOperand.postAnalyze(scope, cg);
			}

			VarType operandType = operand.getType(scope);

			// Проверяем допустимость оператора для типа
			switch (operator) {
				case PLUS:
					if(!operandType.isNumeric() && VarType.CSTR != operandType) {
						markError("Unary " + operator + " requires numeric or string type");
						result = false;
					}
					break;
				case MINUS:
					if(!operandType.isNumeric()) {
						markError("Unary " + operator + " requires numeric type");
						result = false;
					}
					break;
				case BIT_NOT:
					if(!operandType.isInteger()) {
						markError("Bitwise ~ requires integer type");
						result = false;
					}
					break;
				case NOT:
					if(operandType != VarType.BOOL) {
						markError("Logical ! requires boolean type");
						result = false;
					}
					break;
				case PRE_INC:
				case PRE_DEC:
				case POST_INC:
				case POST_DEC:
					if(!operandType.isNumeric()) {
						markError("Increment/decrement requires numeric type");
						result = false;
					}
					if(!(operand instanceof VarFieldExpression) && !(operand instanceof ArrayExpression)) {
						markError("Can only increment/decrement variables");
						result = false;
					}
					// Дополнительная проверка на изменяемость переменной
					if(!(operand instanceof ArrayExpression) && isFinalVariable((VarFieldExpression)operand, scope)) {
						markError("Cannot modify final variable");
						result = false;
					}
					break;
				default:
					markError("Unsupported unary operator: " + operator);
					result = false;
			}
		}
		catch (CompileException e) {
			markError(e.getMessage());
			result = false;
		}
			
		cg.leaveExpression();
		return result;
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
	public void setOperand(ExpressionNode operand) {
		this.operand = operand;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean accumStore) throws Exception {
		return codeGen(cg, parent, false, false, accumStore);
	}
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum) throws Exception {
		CodegenResult result = null;
		
		CGScope brScope = cgScope;
		while(null != brScope && !(brScope instanceof CGBranchScope)) {
			brScope = brScope.getParent();
		}

		if(operand instanceof BinaryExpression) {
			((BinaryExpression)operand).codeGen(cg, null, !isInvert, true, false);
		}
		else if(operand instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)operand;
			if(null != brScope && Operator.NOT == operator) { //TODO вероятно не корректно проверять по brScope(но без блока условий нужен именно emitUnary
				ve.codeGen(cg, !isInvert, !opOr, (CGBranchScope)brScope);
			}
			else {
				ve.codeGen(cg, null, false);
				CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope(CGCellsScope.class);
				cg.eUnary(cgScope, operator, cScope.getCells(), toAccum);
				if(toAccum)	 {
					result = CodegenResult.RESULT_IN_ACCUM;
				}
			}
		}
		else if(operand instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)operand;
			ae.codeGen(cg, null, false);
			CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope(CGCellsScope.class);
			cg.eUnary(cgScope, operator, cScope.getCells(), toAccum);
			if(toAccum)	 {
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}
		else if(operand instanceof UnaryExpression) {
			((UnaryExpression)operand).codeGen(cg, null, !isInvert, opOr, false);
		}
		else {
			throw new CompileException("Unsupported operand '" + operand + " in unary expression");
		}
		
		return result;
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