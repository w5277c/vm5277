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

import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.cg.CodeGenerator;
import java.util.List;
import java.util.Arrays;
import ru.vm5277.common.cg.scopes.CGScope;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;

public class CastExpression extends ExpressionNode {
	private			VarType			sourceType;
	private			ExpressionNode	operand;

	public CastExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, VarType targetType, ExpressionNode operand) {
		super(tb, mc, sp);
		
		this.type = targetType;
		this.operand = operand;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		result&=operand.preAnalyze();
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		result&=operand.declare(scope);
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());
		
		result &=operand.postAnalyze(scope, cg);
		try {
			ExpressionNode optimizedExpr = operand.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				operand = optimizedExpr;
			}

			sourceType = operand.getType();

			if(!canCast(scope, sourceType, type)) {
				markError("Invalid cast from " + sourceType + " to " + type);
				result = false;
			}
		}
		catch (CompileException e) {
			markError(e);
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
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		CGScope cgs = (null==parent ? cgScope : parent);
		
		// Для констант, переменных и полей мы можем задать размер аккумулятора изначально, это даст оптимальный код
		if(operand instanceof LiteralExpression || operand instanceof VarFieldExpression) {
			if(sourceType!=type) {
				cgs.append(cg.accCast(sourceType, type));
			}
			return operand.codeGen(cg, cgs, toAccum);
		}
		else {
			// В остальных случаев обработка выражений не может быть построена с учетом определенного размера аккумулятора, поэтому используем пост приведение
			Object result = operand.codeGen(cg, cgs, toAccum);
			if(sourceType!=type) {
				cgs.append(cg.accCast(sourceType, type));
			}
			return result;
		}
	}

	public ExpressionNode getOperand() {
		return operand;
	}
	public void setOperand(ExpressionNode expr)  {
		operand = expr;
	}
	
	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(operand);
	}
	
	@Override
	public String toString() {
		return "(" + type + ")" + operand;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}