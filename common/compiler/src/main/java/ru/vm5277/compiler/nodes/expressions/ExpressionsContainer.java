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
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		for(ExpressionNode expr : expressions) {
			result&=expr.postAnalyze(scope, cg);
		}
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		for(ExpressionNode expr : expressions) {
			expr.codeOptimization(scope, cg);
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		for(ExpressionNode expr : expressions) {
			Object result = expr.codeGen(cg, parent, toAccum);
			if(toAccum && CodegenResult.RESULT_IN_ACCUM!=result) {
				throw new CompileException("Accum not used for operand:" + expr);
			}
		}
		return (toAccum ? CodegenResult.RESULT_IN_ACCUM : null);
	}		
}
