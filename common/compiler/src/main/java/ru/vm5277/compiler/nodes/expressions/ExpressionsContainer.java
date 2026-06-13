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

import java.util.ArrayList;
import java.util.List;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.compiler.Instance;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class ExpressionsContainer extends ExpressionNode {
	protected	List<ExpressionNode>	expressions	= new ArrayList<>();
	
	public ExpressionsContainer(Instance inst, TokenBuffer tb) {
		super(inst, tb);
	}
	
	public ExpressionsContainer(Instance inst, TokenBuffer tb, SourcePosition sp) {
		super(inst, tb, sp);
	}

	public void add(ExpressionNode exprNode) {
		expressions.add(exprNode);
	}
	
	public List<ExpressionNode> getExprs() {
		return expressions;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		for(ExpressionNode expr : expressions) {
			result&=expr.preAnalyze();
		}
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		for(ExpressionNode expr : expressions) {
			result&=expr.declare(scope);
		}
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());

		for(int i=0; i<expressions.size(); i++) {
			ExpressionNode expr = expressions.get(i);
			result&=expr.postAnalyze(scope, cg, cgScope);
			if(result) {
				// Резолвинг QualifiedPathExpression
				ExpressionNode resolved = resolveQualifiedPathExpr(expr);
				if(null!=resolved) {
					expr = resolved;
					expressions.set(i, resolved);
				}
			}
		}
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		for(int i=0; i<expressions.size(); i++) {
			ExpressionNode expr = expressions.get(i);
			expr.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					if(optimizedExpr instanceof ExpressionsContainer) {
						expressions.remove(i);
						expressions.addAll(i, ((ExpressionsContainer)optimizedExpr).getExprs());
					}
					else {
						expressions.set(i, optimizedExpr);
					}
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		for(ExpressionNode expr : expressions) {
			Object result = expr.codeGen(cg, toAccum, excs);
			if(toAccum && CodegenResult.RESULT_IN_ACCUM!=result) {
				throw new CompileException("Accum not used for operand:" + expr);
			}
		}
		return (toAccum ? CodegenResult.RESULT_IN_ACCUM : null);
	}
	
	@Override
	public String toString() {
		return "container";
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
