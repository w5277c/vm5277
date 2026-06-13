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
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.Instance;

public class TernaryExpression extends ExpressionNode {
	private			ExpressionNode	condition;
	private			ExpressionNode	trueExpr;
	private			ExpressionNode	falseExpr;
	private			boolean			alwaysTrue;
	private			boolean			alwaysFalse;
	private			CGBranch		branch		= new CGBranch();

	public TernaryExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode condition, ExpressionNode trueExpr, ExpressionNode falseExpr) {
		super(inst, tb, sp);

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
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());
		
		// Проверяем условие и ветки
		result&=condition.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(condition);
			if(null!=resolved) {
				condition = resolved;
			}
		}		

		result&=trueExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(trueExpr);
			if(null!=resolved) {
				trueExpr = resolved;
			}
		}		

		result&=falseExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(falseExpr);
			if(null!=resolved) {
				falseExpr = resolved;
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
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		condition.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedExpr = condition.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				condition = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
		
		trueExpr.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedExpr = trueExpr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				trueExpr = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}

		falseExpr.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedExpr = falseExpr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				falseExpr = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
	}

	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone) return null;
		cgDone = true;

		//CGScope cgs = (null==parent ? cgScope : parent);
		cgScope.setBranch(branch);
		
		if(alwaysTrue) {
			return trueExpr.codeGen(cg, toAccum, excs);
		}
		if(alwaysFalse) {
			return falseExpr.codeGen(cg, toAccum, excs);
		}

		Object obj = condition.codeGen(cg, true, excs);
		if(obj==CodegenResult.TRUE) {
			return trueExpr.codeGen(cg, toAccum, excs);
		}
		else if(obj==CodegenResult.FALSE) {
			return falseExpr.codeGen(cg, toAccum, excs);
		}
		else if(obj==CodegenResult.RESULT_IN_ACCUM) {
			cg.boolAccCond(cgScope, branch, true);
		}
		else if(obj==CodegenResult.RESULT_IN_FLAG) {
			cg.boolFlagCond(cgScope, false, branch);
		}
		else if(obj==CodegenResult.RESULT_IN_INV_FLAG) {
			cg.boolFlagCond(cgScope, true, branch);
		}

		CGLabelScope endScope = new CGLabelScope(null, CGScope.genId(), LabelNames.TERNARY_END, true);
		
		trueExpr.codeGen(cg, toAccum, excs);
		cg.jump(cgScope, endScope);
		cgScope.append(branch.getEnd());
		falseExpr.codeGen(cg, toAccum, excs);
		cgScope.append(endScope);

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