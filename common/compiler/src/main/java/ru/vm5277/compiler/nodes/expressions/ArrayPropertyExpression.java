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
import ru.vm5277.common.ArrayProperties;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.compiler.semantic.Scope;

public class ArrayPropertyExpression extends ExpressionNode {
	private			ExpressionNode		arrNameExpr;
	private	final	ArrayProperties		property;
	private			NewArrayExpression	nae;

	public ArrayPropertyExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode arrName, ArrayProperties property) {
		super(tb, mc);
		
		this.arrNameExpr = arrName;
		this.property = property;
	}
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		switch (property) {
			case LENGTH:
				return VarType.SHORT;
			default:
				throw new CompileException("Unknown array property: " + property);
		}
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		result &= arrNameExpr.preAnalyze();
		
		if(result) {
			if(arrNameExpr instanceof UnresolvedReferenceExpression) {
				UnresolvedReferenceExpression ure = (UnresolvedReferenceExpression)arrNameExpr;
				if("this".equals(ure.getId())) {
					arrNameExpr = new ThisExpression(tb, mc);
				}
				else if(null != VarType.fromClassName(ure.getId())) {
					arrNameExpr = new TypeReferenceExpression(tb, mc, ure.getId());
				}
				else {
					arrNameExpr =  new VarFieldExpression(tb, mc, ure.getId());
				}
			}

			result &= arrNameExpr.preAnalyze();
		}
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		result &= arrNameExpr.declare(scope);
		
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());

		try {
			ExpressionNode optimizedParentScope = arrNameExpr.optimizeWithScope(scope, cg);
			if(null != optimizedParentScope) {
				arrNameExpr = optimizedParentScope;
			}
			
			result &= arrNameExpr.postAnalyze(scope, cg);
			
			if(arrNameExpr.getSymbol() instanceof AstHolder) {
				AstHolder ah = (AstHolder)arrNameExpr.getSymbol();
				if(ah.getNode() instanceof InitNodeHolder) {
					InitNodeHolder inh = (InitNodeHolder)ah.getNode();
					nae = (NewArrayExpression)inh.getInitNode();
				}
			}

		}
		catch (CompileException e) {
            markError(e.getMessage());
            result = false;
        }
		
		cg.leaveExpression();
		return result;
	}
	
	@Override
	public List<AstNode> getChildren() {
		// У этого выражения нет дочерних узлов в AST
		return Arrays.asList();
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		CodegenResult result = null;
		
		CGScope cgs = (null==parent ? cgScope : parent);
		
		arrNameExpr.codeGen(cg, null, false);
		
		//TODO нужно проверить!
		boolean isView = false;
		AstHolder ah = (AstHolder)arrNameExpr.getSymbol();
		if(ah.getNode() instanceof InitNodeHolder) {
			InitNodeHolder inh = (InitNodeHolder)ah.getNode();
			if(inh.getInitNode() instanceof ArrayExpression) {
				isView = true;
			}
		}

		if(toAccum) {
			switch (property) {
				case LENGTH:
					if(!isView && null!=nae.getConstDimensions()) {
						cg.constToAcc(cgs, 0x02, nae.getConstDimensions()[0x00], false);
					}
					else {
						CGCellsScope cScope = (CGCellsScope)arrNameExpr.getSymbol().getCGScope();
						cg.cellsToArrReg(cgs, cScope.getCells());
						cg.arrSizetoAcc(cgs, isView);
					}
					break;
			}
			return CodegenResult.RESULT_IN_ACCUM;
		}

		return result;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + arrNameExpr.toString() + "." + property;
	}
}
