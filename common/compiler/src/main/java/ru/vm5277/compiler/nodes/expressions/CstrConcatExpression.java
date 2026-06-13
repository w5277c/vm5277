/*
 * Copyright 2026 konstantin@5277.ru
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

import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.nodes.expressions.bin.ArithmeticExpression;
import ru.vm5277.compiler.semantic.Scope;

public class CstrConcatExpression extends ExpressionsContainer {
	
	public CstrConcatExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode leftExpr, ExpressionNode rightExpr) {
		super(inst, tb, sp);
		
		addExpr(leftExpr);
		addExpr(rightExpr);
		
		type = VarType.CSTR;
	}
	
	private void addExpr(ExpressionNode expr) {
		if(expr instanceof ArithmeticExpression && Operator.PLUS == ((ArithmeticExpression)expr).getOperator()) {
			ArithmeticExpression ae = ((ArithmeticExpression)expr);
			if(	(ae.getLeft() instanceof LiteralExpression && ae.getLeft().getType().isText()) ||
				(ae.getRight() instanceof LiteralExpression && ae.getRight().getType().isText())) {
				
				addExpr(ae.getLeft());
				addExpr(ae.getRight());
			}
			else {
				super.add(expr);
			}
		}
		else {
			super.add(expr);
		}
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
		
		for(int i=1; i<expressions.size(); i++) {
			ExpressionNode exprNode1 = expressions.get(i-1);
			ExpressionNode exprNode2 = expressions.get(i);
			ExpressionNode exprNode = optimizeStringConcat(exprNode1, exprNode2);
			if(exprNode instanceof LiteralExpression) {
				expressions.set(i-1, exprNode);
				expressions.remove(i);
				exprNode.rePostAnalyze(scope, cg, exprNode1.getCGScope());
			}
		}
	}
	
	@Override
	public String toString() {
		return "cstr container";
	}

	@Override
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}
