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
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.lexer.Delimiter;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class NewArrayExpression extends ExpressionNode {
	private	int						depth;
	private	List<ExpressionNode>	arrDimensions;
	private	ArrayInitExpression		aiExpr;
	private	int[]					constDimensions		= null;

	public NewArrayExpression(	TokenBuffer tb, MessageContainer mc, SourcePosition sp, VarType type, List<ExpressionNode> arrDimensions,
								ArrayInitExpression aiExpr) {
		super(tb, mc, sp);

		this.type = type;
		this.depth = type.getArrayDepth();
		this.arrDimensions = arrDimensions;
		this.aiExpr = aiExpr;

		if(null!=aiExpr) {
			constDimensions = aiExpr.getDimensions();
		}

		CodeGenerator.setArraysUsage();
	}

	public NewArrayExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, List<String> path) throws CompileException {
		super(tb, mc, sp);
		
		arrDimensions = parseArrayDimensions();
		if(tb.match(Delimiter.LEFT_BRACE)) {
			aiExpr = new ArrayInitExpression(tb, mc, sp, type);
		}
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		for(ExpressionNode expr : arrDimensions) {
			if(null!=expr) {
				if(null!=constDimensions) {
					markError("Invalid array initialization: cannot specify both dimension and explicit values");
					result = false;
					break;
				}
				result&=expr.preAnalyze();
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
			if(0!=constDimensions[0x02]) constDepth=3;
			else if(0!=constDimensions[0x01]) constDepth=2;
			else if(0!=constDimensions[0x00]) constDepth=1;

			if(depth!=constDepth) {
				markError("Invalid array initialization: different dimension size");
				result = false;
			}
		}

		if(null!=aiExpr) {
			result&=aiExpr.preAnalyze();
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		for(ExpressionNode expr : arrDimensions) {
			if(null!=expr) {
				result&=expr.declare(scope);
			}
		}
		if(null!=aiExpr) {
			result&=aiExpr.declare(scope);
		}

		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());

		try {
			if(null==constDimensions) {
				constDimensions = new int[]{0, 0, 0};
				for(int i=0; i<depth; i++) {
					ExpressionNode expr = arrDimensions.get(i);
					result&=expr.postAnalyze(scope, cg);
					if(result) {
						// Пытаемся оптимизировать final Var/Field 
						ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
						if(null!=optimizedExpr) {
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
				cg.setFeature(RTOSFeature.OS_ARRAY_3D);
			}
			else {
				switch(depth) {
					case 0x01:
						cg.setFeature(RTOSFeature.OS_ARRAY_1D);
						break;
					case 0x02:
						cg.setFeature(RTOSFeature.OS_ARRAY_2D);
						break;
					default:
						cg.setFeature(RTOSFeature.OS_ARRAY_3D);
				}
			}

			if(null!=aiExpr) {
				result&=aiExpr.postAnalyze(scope, cg);
			}
		}
		catch (CompileException e) {
			markError(e.getMessage());
		}

		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
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
		int result=1;
		for(int i=0; i<depth; i++) {
			result*=constDimensions[i];
		}
		return result;
	}

	public int getDepth() {
		return depth;
	}

	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		CodegenResult result = null;
		excs.setSourcePosition(sp);
		
		if(null==constDimensions) {
			cg.pushStackReg(cgScope);
			for(int i=arrDimensions.size()-1; i>=0; i--) {
				ExpressionNode expr = arrDimensions.get(i);
				if(expr instanceof LiteralExpression) {
					//TODO 0x02 - количество байт под размер массива
					cg.pushConst(cgScope, 0x02, ((LiteralExpression)expr).getNumValue(), false);
				}
				else {
					expr.codeGen(cg, null, true, excs);
					//TODO 0x02 - количество байт под размер массива
					cgScope.append(cg.accCast(null, VarType.SHORT));
					cg.pushAccBE(cgScope, 0x02);
				}
			}
		}
		cgScope.append(cg.eNewArray(type, depth, constDimensions, excs));
		if(null==constDimensions) {
			cg.popStackReg(cgScope);
		}
		if(null!=aiExpr) {
			cg.pushAccBE(cgScope, 0x02);
			aiExpr.codeGen(cg, cgScope, false, excs);
			cg.popAccBE(cgScope, 0x02);
		}

		return CodegenResult.RESULT_IN_ACCUM;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(null!=constDimensions) {
			sb.append("[").append(constDimensions[0]).append("]");
			if(0<constDimensions[1]) sb.append("[").append(constDimensions[1]).append("]");
			if(0<constDimensions[2]) sb.append("[").append(constDimensions[2]).append("]");
		}
		else {
			sb.append("[").append(arrDimensions.get(0)).append("]");
			if(1<arrDimensions.size() && null!=arrDimensions.get(1)) sb.append("[").append(arrDimensions.get(1)).append("]");
			if(2<arrDimensions.size() && null!=arrDimensions.get(2)) sb.append("[").append(arrDimensions.get(2)).append("]");
		}
		return "new " + type + sb.toString();
	}

	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}
}