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
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;

public class UnaryExpression extends ExpressionNode {
    private	final	Operator		operator;
    private			ExpressionNode	operand;
    private			boolean			isUsed		= false;
	
    public UnaryExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, Operator operator, ExpressionNode operand) {
        super(tb, mc, sp);
        
		this.operator = operator;
        this.operand = operand;
    }
    
	private boolean isFinalVariable(VarFieldExpression var, Scope scope) {
/*		Symbol symbol = scope.resolveVFSymbol(var.getValue());
		if(null == symbol) {
			InterfaceScope iScope = scope.getThis().resolveScope(var.getValue());
			if(null != iScope) {
				symbol = new Symbol(var.getValue(), VarType.CLASS, false, false);
			}
		}
		return symbol != null && symbol.isFinal();*/
		return var.getSymbol().isFinal();
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
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		result &= operand.preAnalyze();
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		result&=operand.declare(scope);

		if((operator.isPostfix() || Operator.PRE_DEC == operator || Operator.PRE_INC == operator) && operand instanceof QualifiedPathExpression) {
			((QualifiedPathExpression)operand).setReassigned();
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		
		cgScope = cg.enterExpression(toString());
		//if(null != symbol) symbol.setCGScope(cgScope);
		
		try {
			result&=operand.postAnalyze(scope, cg);
			
			ExpressionNode optimizedExpr = operand.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				operand = optimizedExpr;
			}

			type = operand.getType();
			
			// Проверяем допустимость оператора для типа
			switch (operator) {
				case PLUS:
					if(!type.isNumeric() && VarType.CSTR != type) {
						markError("Unary " + operator + " requires numeric or string type");
						result = false;
					}
					break;
				case MINUS:
					if(!type.isNumeric()) {
						markError("Unary " + operator + " requires numeric type");
						result = false;
					}
					break;
				case BIT_NOT:
					if(!type.isIntegral()) {
						markError("Bitwise ~ requires integer type");
						result = false;
					}
					break;
				case NOT:
					if(type != VarType.BOOL) {
						markError("Logical ! requires boolean type");
						result = false;
					}
					break;
				case PRE_INC:
				case PRE_DEC:
				case POST_INC:
				case POST_DEC:
					if(!type.isNumeric()) {
						markError("Increment/decrement requires numeric type");
						result = false;
					}
					if(!(operand instanceof VarFieldExpression) && !(operand instanceof ArrayExpression)) {
						markError("Can only increment/decrement variables");
						result = false;
					}
					// Дополнительная проверка на изменяемость переменной
					if(	result && !(operand instanceof ArrayExpression) &&
						(operand instanceof LiteralExpression || isFinalVariable((VarFieldExpression)operand, scope))) {
						
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
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);
		
		operand.codeOptimization(scope, cg);
		
		try {
			ExpressionNode optimizedExpr = operand.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				operand = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
		
		cg.setScope(oldScope);
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean accumStore, CGExcs excs) throws CompileException {
		return codeGen(cg, parent, false, false, accumStore, excs);
	}
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		Object result = null;
		excs.setSourcePosition(sp);
		
		CGScope cgs = null == parent ? cgScope : parent;
		
		CGBranch branch = null;
		CGScope _scope = cgScope;
		while(null!=_scope) {
			branch = _scope.getBranch();
			if(null!=branch) break;
			_scope = _scope.getParent();
		}

		boolean newBranch = false;
		if(Operator.NOT==operator && null==branch) { // Встретили первую логическую операцию, необходим branch
			branch = new CGBranch();
			cgs.setBranch(branch);
			newBranch = true;
		}

		if(operand instanceof BinaryExpression) {
			if(Operator.NOT==operator) {
				((BinaryExpression)operand).codeGen(cg, cgs, !isInvert, !opOr, toAccum, excs);
			}
			else {
				if(toAccum) {
					if(CodegenResult.RESULT_IN_ACCUM!=((BinaryExpression)operand).codeGen(cg, cgs, isInvert, opOr, true, excs)) {
						throw new CompileException("Accum not used for operand:" + operand);					
					}
					cg.eUnary(cgs, operator, new CGCells(CGCells.Type.ACC), toAccum, excs);
				}
				else {
					throw new CompileException("Unsupported operand '" + operand + " in unary expression");
				}
			}
			result = CodegenResult.RESULT_IN_ACCUM;
		}
		else if(operand instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)operand;
			if(Operator.NOT==operator) {
				ve.codeGen(cg, cgs, !isInvert, !opOr, branch, excs);
			}
			else {
				ve.codeGen(cg, cgs, false, excs);
				CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope(CGCellsScope.class);
				cg.eUnary(cgs, operator, cScope.getCells(), toAccum, excs);
				if(toAccum)	 {
					result = CodegenResult.RESULT_IN_ACCUM;
				}
			}
		}
		else if(operand instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)operand;
			ae.codeGen(cg, null, false, excs);
			CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope(CGCellsScope.class);
			cg.eUnary(cgs, operator, cScope.getCells(), toAccum, excs);
			if(toAccum)	 {
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}
		else if(operand instanceof UnaryExpression) {
			if(Operator.NOT==operator) {
				((UnaryExpression)operand).codeGen(cg, cgs, !isInvert, !opOr, false, excs);
			}
			else {
				((UnaryExpression)operand).codeGen(cg, cgs, !isInvert, opOr, false, excs);
			}
		}
		else {
			throw new CompileException("Unsupported operand '" + operand + " in unary expression");
		}
		
		if(newBranch && branch.isUsed()) {
			cg.constToAcc(cgs, 1, 1, false);
			CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
			cg.jump(cgs, lbScope);
			cgs.append(((CGBranch)branch).getEnd());
			cg.constToAcc(cgs, 1, 0, false);
			cgs.append(lbScope);
		}

		return result;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(operand);
	}

	@Override
	public String toString() {
		return operator.toString() + operand;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}