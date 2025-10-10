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
import ru.vm5277.common.RTOSFeature;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class NewArrayExpression extends ExpressionNode {
	private	VarType					type;
	private	int						depth;
	private	List<ExpressionNode>	arrDimensions;
	private	ArrayInitExpression		aiExpr;
	private	int[]					constDimensions		= null;
	
	public NewArrayExpression(TokenBuffer tb, MessageContainer mc, VarType type, List<ExpressionNode> arrDimensions, ArrayInitExpression aiExpr) {
        super(tb, mc);
		
		this.type = type;
		this.depth = type.getArrayDepth();
		this.arrDimensions = arrDimensions;
		this.aiExpr = aiExpr;
		
		if(null != aiExpr) {
			constDimensions = aiExpr.getDimensions();
		}
		
		CodeGenerator.setArraysUsage();
    }
	
	@Override
	public VarType getType(Scope scope) throws CompileException {
		return type;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		
		for(ExpressionNode expr : arrDimensions) {
			if(null!=expr) {
				if(null!=constDimensions) {
					markError("Invalid array initialization: cannot specify both dimension and explicit values");
					result = false;
					break;
				}
				result &= expr.preAnalyze();
			}
			else if(null==constDimensions) {
				markError("Invalid array initialization: missing dimension size or initializer");
				result = false;
				break;
			}
		}

		if(null!=constDimensions) {
			// Определяю вложенность
			int constDepth=-1;
			if(0 != constDimensions[0x02]) constDepth=3;
			else if(0 != constDimensions[0x01]) constDepth=2;
			else if(0 != constDimensions[0x00]) constDepth=1;

			if(depth!=constDepth) {
				markError("Invalid array initialization: different dimension size");
				result = false;
			}
		}
		
		if(null != aiExpr) {
			result &= aiExpr.preAnalyze();
		}
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		for(ExpressionNode expr : arrDimensions) {
			if(null!=expr) {
				result &= expr.declare(scope);
			}
		}
		if(null != aiExpr) {
			result &= aiExpr.declare(scope);
		}
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());
		
		try {
			if(null==constDimensions) {
				constDimensions = new int[]{0, 0, 0};
				for(int i=0; i<depth; i++) {
					ExpressionNode expr = arrDimensions.get(i);
					result&=expr.postAnalyze(scope, cg);
					if(expr instanceof UnresolvedReferenceExpression) {
						expr = ((UnresolvedReferenceExpression)expr).getResolvedExpr();
						arrDimensions.set(i, expr);
					}

					if(result) {
						// Пытаемся оптимизировать final Var/Field 
						ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
						if(null != optimizedExpr) {
							optimizedExpr.postAnalyze(scope, cg);
							expr = optimizedExpr;
							arrDimensions.set(i, expr);
						}

						if(0==i || null!=constDimensions) {
							if(expr instanceof LiteralExpression) {
								constDimensions[i] = (int)((LiteralExpression)expr).getNumValue();
							}
							else {
								constDimensions = null;
							}
						}
					}
				}
			}
			
			if(null==constDimensions) {
				cg.setRTOSFeature(RTOSFeature.OS_ARRAY_3D);
			}
			else {
				switch(depth) {
					case 0x01:
						cg.setRTOSFeature(RTOSFeature.OS_ARRAY_1D);
						break;
					case 0x02:
						cg.setRTOSFeature(RTOSFeature.OS_ARRAY_2D);
						break;
					default:
						cg.setRTOSFeature(RTOSFeature.OS_ARRAY_3D);
				}
			}
			
			if(null != aiExpr) {
				result &= aiExpr.postAnalyze(scope, cg);
			}
		}
		catch (CompileException e) {
			markError(e.getMessage());
		}
		
		cg.leaveExpression();
		return result;
	}
	
	public List<ExpressionNode> getDimensions() {
		if(null==constDimensions) {
			return arrDimensions;
		}
		List<ExpressionNode> result = new ArrayList<>();
		for(int i=0; i<0x03; i++) {
			if(0==constDimensions[i]) break;
			result.add(new LiteralExpression(tb, mc, constDimensions[i]));
		}
		return result;
	}
	
	public ArrayInitExpression getInitializer() {
		return aiExpr;
	}
	
	public int[] getConstDimensions() {
		return constDimensions;
	}
	
	public Integer getConstSize() {
		if(null==constDimensions) return null;
		int result = 1;
		for(int i=0; i<depth; i++) {
			result *= constDimensions[i];
		}
		return result;
	}
	
	public int getDepth() {
		return depth;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		CodegenResult result = null;
		
		if(null==constDimensions) {
			cg.pushStackReg(cgScope);
			for(int i=arrDimensions.size()-1; i>=0; i--) {
				ExpressionNode expr = arrDimensions.get(i);
				if(expr instanceof LiteralExpression) {
					//TODO 0x02 - количество байт под размер массива
					cg.pushConst(cgScope, 0x02, ((LiteralExpression)expr).getNumValue(), false);
				}
				else {
					expr.codeGen(cg, null, true);
					//TODO 0x02 - количество байт под размер массива
					cgScope.append(cg.accCast(null, VarType.SHORT));
					cg.pushAccBE(cgScope, 0x02);
				}
			}
		}
		cgScope.append(cg.eNewArray(type, depth, constDimensions));
		if(null==constDimensions) {
			cg.popStackReg(cgScope);
		}
		if(null != aiExpr) {
			cg.pushAccBE(cgScope, 0x02);
			aiExpr.codeGen(cg, cgScope, false);
			cg.popAccBE(cgScope, 0x02);
		}
	
		return CodegenResult.RESULT_IN_ACCUM;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}