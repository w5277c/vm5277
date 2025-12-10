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

import java.util.List;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.InitNodeHolder;

public class ArrayExpression extends ExpressionNode {
	private			ExpressionNode			targetExpr;
	private			List<ExpressionNode>	indexExprs;
	private			int[]					constIndexes= new int[]{-1, -1, -1};
	private	final	CGArrCells				arrCells;

	public ArrayExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode targetExpr, List<ExpressionNode> indexExprs)
																																	throws CompileException {
		super(tb, mc, sp);
	
		this.targetExpr = targetExpr;
		
		if(0>=indexExprs.size() || 3<indexExprs.size()) {
			throw new CompileException("Invalid array dimensions - expected 1 to 3 dimensions, but got " + indexExprs.size());
		}

		this.indexExprs = indexExprs;
		
		arrCells = new CGArrCells(indexExprs.size());
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		for(ExpressionNode indexExpr : indexExprs) {
			result&=indexExpr.preAnalyze();
		}
		
		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		for(ExpressionNode indexExpr : indexExprs) {
			result&=indexExpr.declare(scope);
		}

		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());

		VarType vt = targetExpr.getType();
		for(int i=0;i<indexExprs.size(); i++) {
			vt = vt.getElementType();
		}
		type = vt;
		int cellSize = (-1==type.getSize() ? cg.getResId() : type.getSize());
		cgScope = cg.enterArrayExpression(type, cellSize, arrCells);
		
		result&=targetExpr.postAnalyze(scope, cg);

		if(result) {
			try {
				ExpressionNode optimizedExpr = targetExpr.optimizeWithScope(scope, cg);
				if(null!=optimizedExpr) {
					targetExpr = optimizedExpr;
				}

				// Проверяем тип массива
				if(!targetExpr.getType().isArray()) {
					markError("Cannot index non-array type: " + vt);
					result = false;
				}
				if(result) {
					symbol = new Symbol("arrayAccess", type);
					symbol.setCGScope(cgScope);
					arrCells.setSize(cellSize);

					for(int i=0; i<indexExprs.size(); i++) {
						ExpressionNode expr = indexExprs.get(i);
						result&=expr.postAnalyze(scope, cg);
						if(result) {
							optimizedExpr = expr.optimizeWithScope(scope, cg);
							if(null != optimizedExpr) {
								optimizedExpr.postAnalyze(scope, cg);
								expr = optimizedExpr;
								indexExprs.set(i, optimizedExpr);
							}

							// Проверяем тип индекса
							VarType indexType = expr.getType();
							if (!indexType.isIntegral()) {
								markError("Array index must be integer, got " + indexType);
								result = false;
							}
							else if(0x02<indexType.getSize()) {
								markError("Array index type too large, got " + indexType);
								result = false;
							}
						}
					}


					if(result) {
						if(targetExpr.getSymbol() instanceof AstHolder) {
							int[] dimensions = getTargetDimensions();
							if(null != dimensions) {
								for(int i=0; i<indexExprs.size(); i++) {
									ExpressionNode indexExpr = indexExprs.get(i);
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
		}
		
		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	public ExpressionNode getPathExpr() {
		return targetExpr;
	}

	public List<ExpressionNode> getIndexesExpr() {
		return indexExprs;
	}
	
	public int getDepth() {
		return indexExprs.size();
	}
	
//	public int[] getIndexesConst() {
//		return constIndexes;
//	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		CodegenResult result = null;
		excs.setSourcePosition(sp);
		
		CGScope cgs = (null==parent ? cgScope : parent);

		// Как минимум необходим если targetExpr instanceof MethodCallExpr(MethodCallExpr на toAccum не смотрит, здесь главный тип метода(возвращаемое значение в accum))
		targetExpr.codeGen(cg, cgs, false, excs);

		CGCellsScope cScope = (CGCellsScope)targetExpr.getSymbol().getCGScope(CGCellsScope.class);
//		int accSize = cg.getAccumSize();

		boolean makeView = (null!=parent && parent instanceof CGCellsScope && ((CGCellsScope)parent).getType().isArray());
		boolean isView = false;
		if(targetExpr.getSymbol() instanceof AstHolder) {
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
			
			for(int i=0; i<indexExprs.size(); i++) {
				ExpressionNode indexExpr = indexExprs.get(i);
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
			for(int i=0; i<indexExprs.size(); i++) {
				ExpressionNode indexExpr = indexExprs.get(i);
				if(indexExpr instanceof LiteralExpression && ((LiteralExpression)indexExpr).isInteger()) {
					int index = (int)((LiteralExpression)indexExpr).getNumValue();
					cg.pushConst(cgs, 0x02, index, false);
				}
				else {
					if(indexExpr instanceof ArrayExpression) {
						indexExpr.codeGen(cg, cgs, false, excs);
						cg.pushCells(cgs, 0x02, new CGCells(CGCells.Type.ARRAY,
															(-1==indexExpr.getType().getSize() ? cg.getRefSize() : indexExpr.getType().getSize())));
//						cgs.append(cg.accCast(null, VarType.SHORT));
						//0x02 - количество байт под индекс массива
//						cg.pushArrReg(cgs);
					}
					else {
						if(CodegenResult.RESULT_IN_ACCUM == indexExpr.codeGen(cg, cgs, true, excs)) {
							//0x02 - количество байт под размер массива
							cgs.append(cg.accCast(null, VarType.SHORT));
							cg.pushAccBE(cgs, 0x02);
						}
						else {
							cg.pushCells(cgs, 0x02, new CGCells(CGCells.Type.ARRAY,
																(-1==indexExpr.getType().getSize() ? cg.getRefSize() : indexExpr.getType().getSize())));
						}
					}
				}
			}
		}

		//TODO это точно нужно после targetExpr.depCodeGen(cg);? Для массива не нужно, он возвращает ArrReg
//		targetExpr.codeGen(cg, cgs, false);
		
		// Если target - MethodCallExpression то результат расположен в аккумуляторе, иначе ожидаем в ArrReg
		if(targetExpr instanceof MethodCallExpression) {
			cg.accToArrReg(cgs);
		}
		else {
			cg.cellsToArrReg(cgs, cScope.getCells());
		}

		// Формируем код для View
		if(makeView) {
//			cg.arrRegToCells(cgs, new CGCells(CGCells.Type.ACC, 0x02));
//			cg.computeArrCellAddr(cgs, null, arrCells);
			cgs.append(cg.eNewArrView(indexExprs.size()-1, excs));
		}
		else {
			cg.computeArrCellAddr(cgs, null, arrCells, excs);
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
		if(targetExpr.getSymbol() instanceof AstHolder) {
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
		for(int i=0; i<indexExprs.size(); i++) {
			sb.append("[").append(indexExprs.get(i)).append("]");
		}
		return targetExpr.toString() + sb.toString();
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}

}