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
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Instance;
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

	public ArrayExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode targetExpr, List<ExpressionNode> indexExprs)
																																	throws CompileException {
		super(inst, tb, sp);
	
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());

		VarType vt = targetExpr.getType();
		for(int i=0;i<indexExprs.size(); i++) {
			vt = vt.getElementType();
		}
		type = vt;
		int cellSize = (-1==type.getSize() ? cg.getResId() : type.getSize());

		if(null!=cgScope) cgScope.disable();
		cgScope = cg.enterArrayExpression(parent, type, cellSize, arrCells);
		
		result&=targetExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(targetExpr);
			if(null!=resolved) {
				targetExpr = resolved;
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

				// Направление цикла влияет на кодогенерацию - на порядок размещения индексов в стек
				for(int i=indexExprs.size()-1; i>=0; i--) {
					ExpressionNode expr = indexExprs.get(i);
					result&=expr.postAnalyze(scope, cg, cgScope);
					if(result) {
						// Резолвинг QualifiedPathExpression
						resolved = resolveQualifiedPathExpr(expr);
						if(null!=resolved) {
							expr = resolved;
							indexExprs.set(i, resolved);
						}

						// Проверяем тип индекса
						VarType indexType = expr.getType();
						if (!indexType.isInteger()) {
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
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		targetExpr.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedParentScope = targetExpr.optimizeWithScope(scope, cg);
			if(null!=optimizedParentScope) {
				targetExpr = optimizedParentScope;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
		
		for(int i=indexExprs.size()-1; i>=0; i--) {
			ExpressionNode expr = indexExprs.get(i);
			expr.codeOptimization(scope, cg);
			try {
				ExpressionNode optimizedExpr = expr.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					expr = optimizedExpr;
					indexExprs.set(i, optimizedExpr);
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}
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
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		CodegenResult result = null;
		excs.setSourcePosition(sp);
		
		targetExpr.codeGen(cg, false, excs);

		CGCellsScope cScope = (CGCellsScope)targetExpr.getSymbol().getCGScope(CGCellsScope.class);
		
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
			if(!toAccum) {
				//Похоже вообшще не нужно cg.pushAccBE(cgs, accSize);
			}
			for(int i=indexExprs.size()-1; i>=0; i--) {
				ExpressionNode indexExpr = indexExprs.get(i);
				if(indexExpr instanceof LiteralExpression && ((LiteralExpression)indexExpr).type.isInteger()) {
					int index = (int)((LiteralExpression)indexExpr).getNumValue();
					cg.pushConst(indexExpr.getCGScope(), 0x02, index, false);
				}
				else {
					if(indexExpr instanceof ArrayExpression) {
						indexExpr.codeGen(cg, false, excs);
						cg.pushCells(indexExpr.getCGScope(), 0x02, new CGCells(CGCells.Type.ARRAY,
															(-1==indexExpr.getType().getSize() ? cg.getRefSize() : indexExpr.getType().getSize())));
//						cgs.append(cg.accCast(null, VarType.SHORT));
						//0x02 - количество байт под индекс массива
//						cg.pushArrReg(cgs);
					}
					else {
						boolean optimized = false;
						if(indexExpr instanceof VarFieldExpression) {
							try {
								VarFieldExpression ve = (VarFieldExpression)indexExpr;
								cg.pushCells(ve.getCGScope(), 0x02, ((CGCellsScope)ve.getSymbol().getCGScope()).getCells());
								optimized = true;
							}
							catch(Exception ex) {
								System.out.println("TODO:" + ex.getMessage());
							}
						}
						
						if(!optimized) {
							cg.accumLock(VarType.SHORT);
							if(CodegenResult.RESULT_IN_ACCUM == indexExpr.codeGen(cg, true, excs)) {
								//0x02 - количество байт под размер массива
								cg.pushAccLE(indexExpr.getCGScope(), 0x02);
							}
							else {
								cg.pushCells(indexExpr.getCGScope(), 0x02, new CGCells(CGCells.Type.ARRAY,
																	(-1==indexExpr.getType().getSize() ? cg.getRefSize() : indexExpr.getType().getSize())));
							}
							cg.accumUnlock();
						}
					}
				}
			}
		}

		//TODO это точно нужно после targetExpr.depCodeGen(cg);? Для массива не нужно, он возвращает ArrReg
//		targetExpr.codeGen(cg, cgs, false);
		
		// Если target - MethodCallExpression то результат расположен в аккумуляторе, иначе ожидаем в ArrReg
		if(targetExpr instanceof MethodCallExpression) {
			cg.accToArrReg(cgScope);
		}
		else {
			cg.cellsToArrReg(cgScope, cScope.getCells());
		}

		if(isView) {
			cg.computeArrViewCellsAddr(cgScope, null, arrCells, excs); //Портит аккумулятор (4 байта)
		}
		else {
			cg.computeArrCellAddr(cgScope, null, arrCells, excs);
		}
		if(toAccum) {
			cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ACC), null, (CGArrCells)((CGCellsScope)symbol.getCGScope()).getCells(), type);
//			cg.arrToAcc(cgScope, (CGArrCells)((CGCellsScope)symbol.getCGScope()).getCells(), type.isFixedPoint());
			result = CodegenResult.RESULT_IN_ACCUM;
		}

		if((isView || !arrCells.canComputeStatic()) && !toAccum) {
			//Похоже вообшще не нужно cg.popAccBE(cgs, accSize);
		}

		return result;
	}
	
	public Object codeGen(CodeGenerator cg, boolean toAccum, boolean makeView, CGExcs excs) throws CompileException {
		return makeView ? codeGenView(cg, toAccum, excs) : codeGen(cg, toAccum, excs);
	}

	private Object codeGenView(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		excs.setSourcePosition(sp);
		
		//CGScope cgs = (null==parent ? cgScope : parent);

		// Как минимум необходим если targetExpr instanceof MethodCallExpr(MethodCallExpr на toAccum не смотрит, здесь главный тип метода(возвращаемое значение в accum))
		targetExpr.codeGen(cg, false, excs);

		CGCellsScope cScope = (CGCellsScope)targetExpr.getSymbol().getCGScope(CGCellsScope.class);
//		int accSize = cg.getAccumSize();

		//TODO! boolean makeView = (null!=parent && parent instanceof CGCellsScope && ((CGCellsScope)parent).getType().isArray());
		boolean makeView = false; //!!!
		
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
			for(int i=indexExprs.size()-1; i>=0; i--) {
				ExpressionNode indexExpr = indexExprs.get(i);
				if(indexExpr instanceof LiteralExpression && ((LiteralExpression)indexExpr).type.isInteger()) {
					int index = (int)((LiteralExpression)indexExpr).getNumValue();
					cg.pushConst(indexExpr.getCGScope(), 0x02, index, false);
				}
				else {
					if(indexExpr instanceof ArrayExpression) {
						indexExpr.codeGen(cg, false, excs);
						cg.pushCells(indexExpr.getCGScope(), 0x02, new CGCells(CGCells.Type.ARRAY,
															(-1==indexExpr.getType().getSize() ? cg.getRefSize() : indexExpr.getType().getSize())));
//						cgs.append(cg.accCast(null, VarType.SHORT));
						//0x02 - количество байт под индекс массива
//						cg.pushArrReg(cgs);
					}
					else {
						boolean optimized = false;
						if(indexExpr instanceof VarFieldExpression) {
							try {
								VarFieldExpression ve = (VarFieldExpression)indexExpr;
								cg.pushCells(ve.getCGScope(), 0x02, ((CGCellsScope)ve.getSymbol().getCGScope()).getCells());
								optimized = true;
							}
							catch(Exception ex) {
								System.out.println("TODO:" + ex.getMessage());
							}
						}
						
						if(!optimized) {
							cg.accumLock(VarType.SHORT);
							if(CodegenResult.RESULT_IN_ACCUM == indexExpr.codeGen(cg, true, excs)) {
								//0x02 - количество байт под размер массива
								cg.pushAccLE(indexExpr.getCGScope(), 0x02);
							}
							else {
								cg.pushCells(indexExpr.getCGScope(), 0x02, new CGCells(CGCells.Type.ARRAY,
																	(-1==indexExpr.getType().getSize() ? cg.getRefSize() : indexExpr.getType().getSize())));
							}
							cg.accumUnlock();
						}
					}
				}
			}
		}

		//TODO это точно нужно после targetExpr.depCodeGen(cg);? Для массива не нужно, он возвращает ArrReg
//		targetExpr.codeGen(cg, cgs, false);
		
		// Если target - MethodCallExpression то результат расположен в аккумуляторе, иначе ожидаем в ArrReg
		if(targetExpr instanceof MethodCallExpression) {
			cg.accToArrReg(cgScope);
		}
		else {
			cg.cellsToArrReg(cgScope, cScope.getCells());
		}

		// Формируем код для View
//		cg.arrRegToCells(cgs, new CGCells(CGCells.Type.ACC, 0x02));
//		cg.computeArrCellAddr(cgs, null, arrCells);
			
		if(arrCells.canComputeStatic()) {
			int indexData = arrCells.getIndexConst(0x00);
			if(1<arrCells.getDepth()) {
				indexData = (arrCells.getIndexConst(0x01) * 0x10000);
			}
			//TODO !!!
			cg.constToAcc(cgScope, (arrCells.getDepth())*2, indexData, false);
		}
		cg.eNewArrView(cgScope, indexExprs.size()-1, excs);

		return CodegenResult.RESULT_IN_ACCUM;
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