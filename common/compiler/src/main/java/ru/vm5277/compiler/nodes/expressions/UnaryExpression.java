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
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;

public class UnaryExpression extends ExpressionNode {
    private	final	Operator		operator;
    private			ExpressionNode	operand;
    private			boolean			isUsed		= false;
	
    public UnaryExpression(Instance inst, TokenBuffer tb, SourcePosition sp, Operator operator, ExpressionNode operand) {
        super(inst, tb, sp);
        
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());
		//if(null != symbol) symbol.setCGScope(cgScope);
		
		result&=operand.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(operand);
			if(null!=resolved) {
				operand = resolved;
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
					if(!type.isInteger()) {
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
			
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
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
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean accumStore, CGExcs excs) throws CompileException {
		return codeGen(cg, false, false, accumStore, excs);
	}
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		Object result = null;
		excs.setSourcePosition(sp);
		
		CGBranch branch = null;
		CGScope _scope = cgScope;
		while(null!=_scope) {
			branch = _scope.getBranch();
			if(null!=branch) break;
			_scope = _scope.getParent();
		}

		boolean newBranch = false;
		
// Бага - оставил в виде напоминания
// Убрал - а если это присваивание? Например b1=!b2; И вообще сейчас мне не понятна вообще эта логика с переходом по branch для NOT
//		if(Operator.NOT==operator && null==branch) { // Встретили первую логическую операцию, необходим branch
//			branch = new CGBranch();
//			cgScope.setBranch(branch);
//			newBranch = true;
//		}

		cg.accumLock(type);
		if(operand instanceof BinaryExpression) {
			if(Operator.NOT==operator && null!=branch) {
				((BinaryExpression)operand).codeGen(cg, !isInvert, !opOr, toAccum, excs);
			}
			else {
				if(toAccum) {
					if(CodegenResult.RESULT_IN_ACCUM!=((BinaryExpression)operand).codeGen(cg, isInvert, opOr, true, excs)) {
						throw new CompileException("Accum not used for operand:" + operand);					
					}
					cg.eUnary(cgScope, operator, new CGCells(CGCells.Type.ACC), toAccum, excs);
				}
				else {
					throw new CompileException("Unsupported operand '" + operand + " in unary expression");
				}
			}
			result = CodegenResult.RESULT_IN_ACCUM;
		}
		else if(operand instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)operand;
			if(Operator.NOT==operator && null!=branch) {
				ve.codeGen(cg, !isInvert, !opOr, branch, excs);
			}
			else {
				ve.codeGen(cg, false, excs);
				CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope(CGCellsScope.class);
				cg.eUnary(cgScope, operator, cScope.getCells(), toAccum, excs);
				if(toAccum)	 {
					result = CodegenResult.RESULT_IN_ACCUM;
				}
			}
		}
		else if(operand instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)operand;
			ae.codeGen(cg, false, excs);
			CGCellsScope cScope = (CGCellsScope)operand.getSymbol().getCGScope(CGCellsScope.class);
			cg.eUnary(cgScope, operator, cScope.getCells(), toAccum, excs);
			if(toAccum)	 {
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}
		else if(operand instanceof UnaryExpression) {
			if(Operator.NOT==operator) {
				((UnaryExpression)operand).codeGen(cg, !isInvert, !opOr, false, excs);
			}
			else {
				((UnaryExpression)operand).codeGen(cg, !isInvert, opOr, false, excs);
			}
		}
		else if(operand instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)operand;
			Object mceResult = mce.codeGen(cg, true, excs);
			if(CodegenResult.RESULT_IN_ACCUM==mceResult) {
				cg.eUnary(cgScope, operator, new CGCells(CGCells.Type.ACC), toAccum, excs);
				if(toAccum)	 {
					result = CodegenResult.RESULT_IN_ACCUM;
				}
			}
			else if(CodegenResult.RESULT_IN_FLAG==mceResult) {
				if(Operator.NOT==operator) {
					result = CodegenResult.RESULT_IN_INV_FLAG;
				}
				else {
					throw new CompileException("Unsupported operand '" + operand + " in unary expression", sp);
				}
			}
			else {
				throw new CompileException("Accum not used for operand:" + operand);
			}
		}
		else {
			throw new CompileException("Unsupported operand '" + operand + " in unary expression", sp);
		}
		
		if(newBranch && branch.isUsed()) {
			cg.constToAcc(cgScope, 1, 1, false);
			CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.LOCGIC_END, true);
			cg.jump(cgScope, lbScope);
			cgScope.append(((CGBranch)branch).getEnd());
			cg.constToAcc(cgScope, 1, 0, false);
			cgScope.append(lbScope);
		}
		
		cg.accumUnlock();
		return result;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(operand);
	}

	@Override
	public String toString() {
		return operator.toString() +  "(" + operand + ")";
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}