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
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class ExpressionsContainer extends ExpressionNode {
	private	List<ExpressionNode> expressions		= new ArrayList<>();
	
	public ExpressionsContainer(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
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
		for(int i=0; i<expressions.size(); i++) {
			ExpressionNode expr = expressions.get(i);
			result&=expr.postAnalyze(scope, cg, parent);
			if(result) {
				// Резолвинг QualifiedPathExpression
				ExpressionNode resolved = resolveQualifiedPathExpr(expr);
				if(null!=resolved) {
					expr = resolved;
					expressions.set(i, resolved);
				}
			}
		}
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
					expressions.set(i, optimizedExpr);
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		for(ExpressionNode expr : expressions) {
			Object result = expr.codeGen(cg, parent, toAccum, excs);
			if(toAccum && CodegenResult.RESULT_IN_ACCUM!=result) {
				throw new CompileException("Accum not used for operand:" + expr);
			}
		}
		return (toAccum ? CodegenResult.RESULT_IN_ACCUM : null);
	}		
}
