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
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.CatchBlock;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.compiler.semantic.ExceptionScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.VarSymbol;

public class InstanceOfExpression extends ExpressionNode {
	private	ExpressionNode	leftExpr;	// Проверяемое выражение
	private	ExpressionNode	rightExpr;	// Выражение, возвращающее тип
	private	VarType			leftType;
	private	VarType			rightType;
	private	String			varName;
	private	Symbol			varSymbol;
	private	Scope			scope;
	private	boolean			fulfillsContract;	//Флаг реализации интерфейса
	
	public InstanceOfExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode leftExpr, ExpressionNode rightExpr, String varName) {
		super(tb, mc);
		
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
		this.varName = varName;
		this.type = VarType.BOOL;
	}

	public ExpressionNode getLeft() {
		return leftExpr;
	}

	public ExpressionNode getRight() {
		return rightExpr;
	}

	public ExpressionNode getTypeExpr() {
		return rightExpr;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		if(null==leftExpr || null==rightExpr) {
			markError("Both operands of 'is' must be non-null");
			result = false;
		}

		if(result) {
			if(null != varName && Character.isUpperCase(varName.charAt(0))) {
				addMessage(new WarningMessage("Variable name should start with lowercase letter:" + varName, sp));
			}

			result&=leftExpr.preAnalyze();
			result&=rightExpr.preAnalyze();
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		try {
			result&=leftExpr.declare(scope);
			result&=rightExpr.declare(scope);

			if(result) {
				if(rightExpr instanceof LiteralExpression && ((LiteralExpression)rightExpr).getValue() instanceof VarType) {
					rightType = (VarType)((LiteralExpression)rightExpr).getValue();
				}
				else {
					rightType = rightExpr.getType();//TODO должно быть в post
				}
			
				if(null!=varName) {
					// Создаем final alias, в post установим CGScope от leftExpression
					varSymbol = new Symbol(varName, rightType, true, false);
					if(scope instanceof BlockScope) {
						((BlockScope)scope).addVariable(varSymbol);


						// Этот код был рассчитан только на VarFieldExpression
						/*
						if(leftExpr instanceof VarFieldExpression) {
							VarFieldExpression ve = (VarFieldExpression)leftExpr;
							Symbol vfs = scope.resolveVar(ve.getName());
							if(null==vfs) {
								vfs = scope.resolveField(ve.getName(), false);
							}
							if(null == vfs) {
								markError(new CompileException("var '" + ve.getName() + "' not found"));
								result = false;
							}
							else {
								//Добавляем просто символ, большего мы здесь не знаем
								bScope.addAlias(varName, rightType, vfs);
							}
						}
						//TODO а что если другое выражение или вызов метода?
						*/
					}
					else {
						markError(new CompileException("Unexpected scope '" + scope + "'"));
						result = false;
					}
				}
			}
		}
		catch(CompileException e) {
			markError(e);
			result = false;
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		
		this.scope = scope;
		cgScope = cg.enterExpression(toString());

		result&=leftExpr.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = leftExpr.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					leftExpr = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
				result = false;
			}
		}

		result&=rightExpr.postAnalyze(scope, cg);

		if(VarType.UNKNOWN==rightType) {
			markError("Unknown right-hand side of 'is': " + rightExpr);
			result = false;
		}
		if(VarType.VOID==rightType) {
			markError("Right-hand side of 'is' cant be void");
			result = false;
		}

		leftType=leftExpr.getType();
		if(leftExpr instanceof LiteralExpression) {
			markError("Cannot check type of literals at runtime");
			result = false;
		}

		if(VarType.EXCEPTION==rightType || VarType.EXCEPTION==leftType) {
			markError("'Exception' type cannot be used with 'is' operator");
			result = false;
		}
		
		if(result) {
			// Проверка реализации интерфейса
			if(leftType.isClassType()) {
				if(leftType.getClassName().equals(rightType.getClassName())) {
					fulfillsContract = true;
				}
				else {
					CIScope leftCIS = scope.getThis().resolveCI(leftType.getClassName(), false);
					fulfillsContract = leftCIS.isImplements(rightType);
				}
			}

			
			// Просто копирую CGScope из оригинала
			// Инкремент/декремент счетчика ссылок не нужен, так как используем один CGCells
			if(null!=varName) {
				varSymbol.setCGScope(getSrcSymbol().getCGScope());
			}
		}
		
		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	public VarType getLeftType() {
		return leftType;
	}
	
	public VarType getRightType() {
		return rightType;
	}

	public boolean isFulfillsContract() {
		return fulfillsContract;
	}
	
	public String getVarName() {
		return varName;
	}
	
	private Symbol getSrcSymbol() {
		if(leftExpr instanceof CastExpression) {
			return ((CastExpression)leftExpr).getOperand().getSymbol();
		}
		return leftExpr.getSymbol();
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		//Обходимся без рантайма, пока в левой части примитив или это константа
		//Но нужно вызывать рантайм, если встречаем объект, так как только в рантайм данных есть информация о типе переменной
		
		CGScope cgs = null == parent ? cgScope : parent;

		Symbol varSymbol = getSrcSymbol();
		if(varSymbol.isFinal()) return (isCompatibleWith(scope, varSymbol.getType(), rightType) ? CodegenResult.TRUE : CodegenResult.FALSE);

			leftExpr.codeGen(cg, cgs, false, excs);

			// TODO весь код можно вынести в RTOS j8b утилиты и просто вызывать как функцию
			boolean heapSaved=false;
			if(leftExpr instanceof VarFieldExpression) {
				CGCellsScope cScope = (CGCellsScope)((VarFieldExpression)leftExpr).getSymbol().getCGScope();
				cg.pushHeapReg(cgs);
				heapSaved = true;
				cg.setHeapReg(cgs, cScope.getCells());
			}

			cg.eInstanceof(cgs, rightType);

			if(heapSaved) cg.popHeapReg(cgs);

		return CodegenResult.RESULT_IN_ACCUM;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(leftExpr, rightExpr);
	}
	
	@Override
	public String toString() {
		return leftExpr + " is " + rightExpr + (null != varName ? " as " + varName : "");
	}
    
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}