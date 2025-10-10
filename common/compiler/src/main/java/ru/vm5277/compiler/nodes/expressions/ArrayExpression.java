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

import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.InitNodeHolder;

public class ArrayExpression extends ExpressionNode {
	private			ExpressionNode		targetExpr;
    private			ExpressionNode[]	indexExprs	= new ExpressionNode[0x03];
	private			int[]				constIndexes= new int[]{-1, -1, -1};
	private			VarType				type;
	private			int					depth		= 0;
	private	final	CGArrCells			arrCells;

	public ArrayExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode targetExpr) {
		super(tb, mc);

		this.targetExpr = targetExpr;
	
		for(int i=0; i<3; i++) {
			try {consumeToken(tb, Delimiter.LEFT_BRACKET);} catch(CompileException e){markFirstError(e);} // Потребляем '['
			try {indexExprs[i] = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);} // Парсим выражение-индекс
			try {consumeToken(tb, Delimiter.RIGHT_BRACKET);} catch(CompileException e){markFirstError(e);} // Потребляем ']'
			depth++;
			if(!tb.match(Delimiter.LEFT_BRACKET)) {
				break;
			}
		}
		arrCells = new CGArrCells(depth);
	}

   @Override
	public VarType getType(Scope scope) throws CompileException {
		if(null!=type) return type;

		VarType vt = targetExpr.getType(scope);
		for(int i=0;i<depth; i++) {
			vt = vt.getElementType();
		}
		type = vt;
		return type;
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		
		if (null == targetExpr) {
			markError("Array expression is missing");
			return false;
		}

		if (0==depth) {
			markError("Index expression is missing");
			return false;
		}

		result &= targetExpr.preAnalyze();
		
		for(int i=0; i<depth; i++) {
			result &= indexExprs[i].preAnalyze();
		}
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		
		result &= targetExpr.declare(scope);

		symbol = new Symbol("arrayAccess", type);

		for(int i=0; i<depth; i++) {
			ExpressionNode expr = indexExprs[i];
			result &= expr.declare(scope);
		}

		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		try {
			// Проверяем подвыражения
			result &= targetExpr.postAnalyze(scope, cg);
			if(result) {
				getType(scope);

				cgScope = cg.enterArrayExpression(type, (-1==type.getSize() ? cg.getResId() : type.getSize()), arrCells);
				symbol.setCGScope(cgScope);

				VarType vt = type;
				while(vt.isArray()) {
					vt=vt.getElementType();
				}
				arrCells.setSize(-1==vt.getSize() ? cg.getResId() : vt.getSize());


				for(int i=0; i<depth; i++) {
					ExpressionNode expr = indexExprs[i];
					result &= expr.postAnalyze(scope, cg);
					if(result) {
						if(expr instanceof UnresolvedReferenceExpression) {
							expr = ((UnresolvedReferenceExpression)expr).getResolvedExpr();
							indexExprs[i] = expr;
						}

						// Проверяем тип индекса
						VarType indexType = expr.getType(scope);
						if (!indexType.isInteger()) {
							markError("Array index must be integer, got " + indexType);
							result = false;
						}
						else if(0x02<indexType.getSize()) {
							markError("Array index type too large, got " + indexType);
							result = false;
						}
					}
					if(result) {
						// Пытаемся оптимизировать final Var/Field 
						ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
						if(null != optimizedExpr) {
							optimizedExpr.postAnalyze(scope, cg);
							indexExprs[i] = optimizedExpr;
						}
					}
				}

				// Проверяем тип массива
				VarType arrayType = targetExpr.getType(scope);
				if (!arrayType.isArray()) {
					markError("Cannot index non-array type: " + arrayType);
					result = false;
				}


				if(result) {
					if(targetExpr.getSymbol() instanceof AstHolder) {
						int[] dimensions = getTargetDimensions();
						if(null != dimensions) {
							for(int i=0; i<depth; i++) {
								ExpressionNode indexExpr = indexExprs[i];
								if(indexExpr instanceof LiteralExpression) {
									int index = (int)((LiteralExpression)indexExpr).getNumValue();
									if(0>index) {
										markError("Array index is negative:" + index);
									}
									else if(0!=dimensions[i] &&  index>(dimensions[i]-0x01)) {
										markError("Array index " + index + " out of bounds [0.." + (dimensions[i]-0x01) + "]");
									}
								}
							}
						}
					}
				}
			}
		}
		catch (CompileException e) {
			cg.leaveExpression();
			markError(e.getMessage());
			result = false;
		}
		
		cg.leaveExpression();
		return result;
	}
	
	public ExpressionNode getTargetExpr() {
		return targetExpr;
	}

	public ExpressionNode[] getIndexesExpr() {
		return indexExprs;
	}
	
	public int getDepth() {
		return depth;
	}
	
//	public int[] getIndexesConst() {
//		return constIndexes;
//	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		CodegenResult result = null;

		CGScope cgs = (null==parent ? cgScope : parent);

		// Как минимум необходим если targetExpr instanceof MethodCallExpr(MethodCallExpr на toAccum не смотрит, здесь главный тип метода(возвращаемое значение в accum))
		targetExpr.codeGen(cg, cgs, false);

		CGCellsScope cScope = (CGCellsScope)targetExpr.getSymbol().getCGScope();
		int accSize = cg.getAccumSize();

		boolean makeView = (null!=parent && parent instanceof CGCellsScope && ((CGCellsScope)parent).getType().isArray());
		boolean isView = false;
		if(targetExpr instanceof AstHolder) {
			AstHolder ah = (AstHolder)targetExpr.getSymbol();
			if(ah.getNode() instanceof InitNodeHolder) {
				InitNodeHolder inh = (InitNodeHolder)ah.getNode();
				if(inh.getInitNode() instanceof ArrayExpression) {
					isView = true;
				}
			}
		}

		if(!isView) {
			arrCells.setDimensionsConst(getTargetDimensions());
			
			for(int i=0; i<depth; i++) {
				ExpressionNode indexExpr = indexExprs[i];
				if(indexExpr instanceof LiteralExpression) {
					arrCells.setIndexConst(i, (int)((LiteralExpression)indexExpr).getNumValue());
				}
				else if(indexExpr instanceof VarFieldExpression) {
					arrCells.setIndexCells(i, ((CGCellsScope)indexExpr.getSymbol().getCGScope()).getCells());
				}
			}
		}

		if(isView || !arrCells.canComputeStatic()) {
			if(!toAccum && !makeView) {
				//Похоже вообшще не нужно cg.pushAccBE(cgs, accSize);
			}
			for(int i=0; i<depth; i++) {
				ExpressionNode indexExpr = indexExprs[i];
				if(indexExpr instanceof LiteralExpression && ((LiteralExpression)indexExpr).isInteger()) {
					int index = (int)((LiteralExpression)indexExpr).getNumValue();
					cg.pushConst(cgs, 0x02, index, false);
				}
				else {
					if(indexExpr instanceof ArrayExpression) {
						indexExpr.codeGen(cg, cgs, false);
						cg.pushCells(cgs, 0x02, new CGCells(CGCells.Type.ARRAY,
															(-1==indexExpr.getType(null).getSize() ? cg.getRefSize() : indexExpr.getType(null).getSize())));
//						cgs.append(cg.accCast(null, VarType.SHORT));
						//0x02 - количество байт под индекс массива
//						cg.pushArrReg(cgs);
					}
					else {
						if(CodegenResult.RESULT_IN_ACCUM == indexExpr.codeGen(cg, cgs, true)) {
							//0x02 - количество байт под размер массива
							cgs.append(cg.accCast(null, VarType.SHORT));
							cg.pushAccBE(cgs, 0x02);
						}
						else {
							cg.pushCells(cgs, 0x02, new CGCells(CGCells.Type.ARRAY,
																(-1==indexExpr.getType(null).getSize() ? cg.getRefSize() : indexExpr.getType(null).getSize())));
						}
					}
				}
			}
		}

		//TODO это точно нужно после targetExpr.depCodeGen(cg);?
//		targetExpr.codeGen(cg, cgs, false);
		
		cg.cellsToArrReg(cgs, cScope.getCells());

		// Формируем код для View
		if(makeView) {
//			cg.arrRegToCells(cgs, new CGCells(CGCells.Type.ACC, 0x02));
//			cg.computeArrCellAddr(cgs, null, arrCells);
			cgs.append(cg.eNewArrView(depth-1));
		}
		else {
			cg.computeArrCellAddr(cgs, null, arrCells);
			if(toAccum) {
				cg.arrToAcc(cgs, (CGArrCells)((CGCellsScope)symbol.getCGScope()).getCells());
				result = CodegenResult.RESULT_IN_ACCUM;
			}
		}

		if((isView || !arrCells.canComputeStatic()) && !toAccum && !makeView) {
			//Похоже вообшще не нужно cg.popAccBE(cgs, accSize);
		}
		return result;
	}
	
	private int[] getTargetDimensions() {
		if(targetExpr instanceof AstHolder) {
			AstHolder ah = (AstHolder)targetExpr.getSymbol();
			if(ah.getNode() instanceof InitNodeHolder) {
				InitNodeHolder inh = (InitNodeHolder)ah.getNode();
				if(inh.getInitNode() instanceof NewArrayExpression) {
					return ((NewArrayExpression)inh.getInitNode()).getConstDimensions();
				}
				else if(inh.getInitNode() instanceof ArrayExpression) {
					return ((ArrayExpression)inh.getInitNode()).getTargetDimensions();
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<depth; i++) {
			sb.append("[").append(indexExprs[i]).append("]");
		}
		return getClass().getSimpleName() + " " + targetExpr.toString() + sb.toString();
	}
}