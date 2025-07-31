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
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InterfaceSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class InstanceOfExpression extends ExpressionNode {
	private	final	ExpressionNode	leftExpr;		// Проверяемое выражение
	private	final	ExpressionNode	rightExpr;	// Выражение, возвращающее тип
	private			VarType			leftType;
	private			VarType			rightType;
	private			String			varName;
	private			Scope			scope;
	private			boolean			fulfillsContract;	//Флаг реализации интерфейса
	private			boolean			isUsed				= false;	
	
	public InstanceOfExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode leftExpr, ExpressionNode rightExpr, String varName) {
		super(tb, mc);
		this.leftExpr = leftExpr;
		this.rightExpr = rightExpr;
		this.varName = varName;
	}

	@Override
	public VarType getType(Scope scope) throws CompileException {
		// Результат instanceof всегда boolean
		return VarType.BOOL;
	}

	public ExpressionNode getLeft() {
		return leftExpr;
	}

	public ExpressionNode getTypeExpr() {
		return rightExpr;
	}

	@Override
	public boolean preAnalyze() {
		if (leftExpr == null || rightExpr == null) {
			markError("Both operands of 'is' must be non-null");
			return false;
		}
		
		if(null != varName && Character.isUpperCase(varName.charAt(0))) {
			addMessage(new WarningMessage("Variable name should start with lowercase letter:" + varName, sp));
		}
		
		return leftExpr.preAnalyze() && rightExpr.preAnalyze();
	}
	
	@Override
	public boolean declare(Scope scope) {
		try {
			if(rightExpr instanceof LiteralExpression && ((LiteralExpression)rightExpr).getValue() instanceof VarType) {
				rightType = (VarType)((LiteralExpression)rightExpr).getValue();
			}
			else {
				rightType = rightExpr.getType(scope);
			}
			
			if(null != varName) {
				if(scope instanceof BlockScope) {
					BlockScope bScope = (BlockScope)scope;
					
					if(leftExpr instanceof VarFieldExpression) {
						VarFieldExpression ve = (VarFieldExpression)leftExpr;
						Symbol vSymbol = scope.resolve(ve.getValue());
						if(null == vSymbol) {
							markError(new CompileException("var '" + ve.getValue() + "' not found"));
							return false;
						}
						//Добавляем просто символ, большего мы здесь не знаем
						bScope.addAlias(varName, rightType, vSymbol);
					}
					//TODO а что если другое выражение или вызов метода?
				}
				else {
					markError(new CompileException("Unexpected scope '" + scope + "'"));
					return false;
				}
			}
		}
		catch(CompileException e) {
			markError(e);
			return false;
		}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		this.scope = scope;
		try {
			cgScope = cg.enterExpression();
			
			if (!leftExpr.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}

			// Анализируем правую часть (тип)
			if (!rightExpr.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}

			if(VarType.UNKNOWN == rightType) {
				markError("Unknown right-hand side of 'is': " + rightExpr);
				cg.leaveExpression();
				return false;
			}
			if(VarType.VOID == rightType) {
				markError("Right-hand side of 'is' cant be void");
				cg.leaveExpression();
				return false;
			}

			leftType = leftExpr.getType(scope);
			if (leftExpr instanceof LiteralExpression) {
				markError("Cannot check type of literals at runtime");
				cg.leaveExpression();
				return false;
			}
			
			// Проверка реализации интерфейса
			if (leftType.isClassType()) {
				if(leftType.getClassName().equals(rightType.getClassName())) {
					fulfillsContract = true;
				}
				else {
					ClassScope leftClass = scope.getThis().resolveClass(leftType.getClassName());
					InterfaceSymbol rightInterface = scope.getThis().resolveInterface(rightType.getClassName());

					
					if (null != leftClass && null != rightInterface) {
						fulfillsContract = leftClass.getInterfaces().containsKey(rightInterface.getName());
					}
				}
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
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		//Обходимся без рантайма, пока в левой части примитив или это константа
		//Но нужно вызывать рантайм, если в встречаем объект, так как только в рантайм данных есть информация о типе переменной
		
		Symbol  varSymbol = ((VarFieldExpression)leftExpr).getSymbol();
		if(varSymbol.isFinal()) {
			return isCompatibleWith(scope, varSymbol.getType(), rightType);
		}

		leftExpr.codeGen(cg);
		//TODO long objectOp = cg.getAcc(); //???
		//cg.setAcc(cgScope, 0x01, cg.emitInstanceof(objectOp, (int)rightType.getId()) ? 0x01 : 0x00);
		//cg.loadConstToAcc(cgScope, 0x01, cg.emitInstanceof(objectOp, (int)rightType.getId()) ? 0x01 : 0x00);
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(leftExpr, rightExpr);
	}
	
	@Override
	public String toString() {
		return leftExpr + " is " + rightExpr;
	}
}