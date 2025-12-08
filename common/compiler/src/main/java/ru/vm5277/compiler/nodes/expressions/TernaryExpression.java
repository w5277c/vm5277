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
import ru.vm5277.common.VarType;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;

public class TernaryExpression extends ExpressionNode {
	private			ExpressionNode	condition;
	private			ExpressionNode	trueExpr;
	private			ExpressionNode	falseExpr;
	private			boolean			alwaysTrue;
	private			boolean			alwaysFalse;
	private			CGBranch		branch		= new CGBranch();

	public TernaryExpression(	TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode condition, ExpressionNode trueExpr,
								ExpressionNode falseExpr) {
		super(tb, mc, sp);

		this.condition = condition;
		this.trueExpr = trueExpr;
		this.falseExpr = falseExpr;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		if(null==condition || null==trueExpr || null==falseExpr) {
			markError("All parts of ternary expression must be non-null");
			result = false;
		}

		if(result) {
			result&=condition.preAnalyze();
			result&=trueExpr.preAnalyze();
			result&=falseExpr.preAnalyze();
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		result&=condition.declare(scope);
		result&=trueExpr.declare(scope);
		result&=falseExpr.declare(scope);

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());
		
		// Проверяем условие и ветки
		result&=condition.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = condition.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					condition = optimizedExpr;
				}
			}
			catch (CompileException e) {
				markError(e);
				result = false;
			}
		}		

		result&=trueExpr.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = trueExpr.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					trueExpr = optimizedExpr;
				}
			}
			catch (CompileException e) {
				markError(e);
				result = false;
			}
		}		

		result&=falseExpr.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = falseExpr.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					falseExpr = optimizedExpr;
				}
			}
			catch (CompileException e) {
				markError(e);
				result = false;
			}
		}		

		// Находим более общий тип
		VarType trueType = trueExpr.getType();
		VarType falseType = falseExpr.getType();
		type = trueType.getSize() >= falseType.getSize() ? trueType : falseType;

		// Проверяем тип условия
		VarType condType = condition.getType();
		if(VarType.BOOL!=condType) {
			markError("Condition must be boolean, got: " + condType);
			result = false;
		}

		// Проверяем совместимость типов веток
		if (!isCompatibleWith(scope, trueType, falseType)) {
			markError("Incompatible types in branches: " + trueType + " and " + falseType);
			result = false;
		}
		
		if(result) {
			if(condition instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)condition;
				if(VarType.BOOL==le.getType()) {
					if((boolean)le.getValue()) {
						alwaysTrue = true;
					}
					else {
						alwaysFalse = true;
					}
				}
			}
		}
		
		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		CGScope cgs = (null==parent ? cgScope : parent);
		cgs.setBranch(branch);
		
		if(alwaysTrue) {
			return trueExpr.codeGen(cg, cgs, toAccum, excs);
		}
		if(alwaysFalse) {
			return falseExpr.codeGen(cg, cgs, toAccum, excs);
		}

		Object obj = condition.codeGen(cg, cgs, true, excs);
		if(obj==CodegenResult.TRUE) {
			return trueExpr.codeGen(cg, cgs, toAccum, excs);
		}
		else if(obj==CodegenResult.FALSE) {
			return falseExpr.codeGen(cg, cgs, toAccum, excs);
		}
		else if(obj==CodegenResult.RESULT_IN_ACCUM) {
			cg.boolCond(cgs, branch, true);
		}

		CGLabelScope endScope = new CGLabelScope(null, CGScope.genId(), LabelNames.TERNARY_END, true);
		
		trueExpr.codeGen(cg, cgs, toAccum, excs);
		cg.jump(cgs, endScope);
		cgs.append(branch.getEnd());
		falseExpr.codeGen(cg, cgs, toAccum, excs);
		cgs.append(endScope);

		return (toAccum ? CodegenResult.RESULT_IN_ACCUM : null);
	}
	
	// Геттеры
	public ExpressionNode getCondition() {
		return condition;
	}

	public ExpressionNode getTrueExpr() {
		return trueExpr;
	}

	public ExpressionNode getFalseExpr() {
		return falseExpr;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(condition, trueExpr, falseExpr);
	}

	@Override
	public String toString() {
		return condition + " ? " + trueExpr + " : " + falseExpr;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + condition + " ? " + trueExpr + " : " + falseExpr;
	}
}