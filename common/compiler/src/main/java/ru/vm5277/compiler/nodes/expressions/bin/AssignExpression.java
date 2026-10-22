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

package ru.vm5277.compiler.nodes.expressions.bin;

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.compiler.Instance;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.Scope;

public class AssignExpression extends BinaryExpression {
    AssignExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right) {
        super(inst, tb, sp, left, op, right);
    }
    
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		this.scope = scope;
		cgScope = cg.enterExpression(parent, sp, cgScope, toString());

		result&=rightExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(rightExpr);
			if(null!=resolved) {
				rightExpr = resolved;
			}
		}

		// Анализ операндов
		result&=leftExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(leftExpr);
			if(null!=resolved) {
				leftExpr = resolved;
			}
		}

		if(result) {
			// Проверка final переменных
			if(leftExpr.getSymbol().isFinal()) {
				markError("Cannot assign to final var/field: " + leftExpr);
				result = false;
			}
		}
		
		if(result) {
			if(leftExpr instanceof VarFieldExpression) {
				if(rightExpr instanceof UnaryExpression) {
					Operator unaryOp = ((UnaryExpression)rightExpr).getOperator();
					if(Operator.MINUS==unaryOp && !rightExpr.getType().isFixedPoint()) {
						if(StrictLevel.STRONG==inst.getStrictLevel()) {
							markError("Unary " + ((UnaryExpression)rightExpr).getOperator() + " requires fixed type");
							result = false;
						}
						else if(StrictLevel.LIGHT==inst.getStrictLevel()) {
							markWarning("Unary " + ((UnaryExpression)rightExpr).getOperator() + " requires fixed type");
						}
					}
					else if(Operator.PRE_INC==unaryOp || Operator.PRE_DEC==unaryOp || Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
						UnaryExpression ue = (UnaryExpression)rightExpr;
						if(	ue.getOperand() instanceof VarFieldExpression &&
							((VarFieldExpression)leftExpr).getName().equals(((VarFieldExpression)ue.getOperand()).getName())) {
							
							if(StrictLevel.STRONG==inst.getStrictLevel()) {
								if(Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
									markError("Redudnand statement has no practical effect");
								}
								else {
									markError("Redudnand statement");
								}
								result = false;
							}
							else if(StrictLevel.LIGHT == inst.getStrictLevel()) {
								if(Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
									markWarning("Redudnand statement has no practical effect");
									disable();
								}
								else {
									markWarning("Redudnand statement");
								}
							}
						}
					}
				}
			}
			else if(leftExpr instanceof ArrayExpression) {
				//TODO проверить
				if(((ArrayExpression)leftExpr).getDepth()!=((ArrayExpression)leftExpr).getPathExpr().getType().getArrayDepth()) {
					markError("Array assignment dimension mismatch");
					result = false;
				}
			}
			else {
				markError("Invalid left operand for assignment: " + leftExpr);
				result = false;
			}
		}
		
		if(result) {
			if(leftExpr instanceof VarFieldExpression && rightExpr instanceof VarFieldExpression) {
				if(((VarFieldExpression)leftExpr).isSameVarFiled((VarFieldExpression)rightExpr)) {
					markWarning("Self-assignment for '" + ((VarFieldExpression)rightExpr).getName() + "' has no effect");
					disable();
				}
			}
		}

		if(result) {
			leftType = leftExpr.getType();
			rightType = rightExpr.getType();

			// Если один из операндов FIXED, результат FIXED
			if(leftType==VarType.FIXED || rightType==VarType.FIXED) {
				type = VarType.FIXED;
			}
			else {
				// Для арифметических операций возвращаем более широкий тип
				type = leftType.getSize()>=rightType.getSize() ? leftType : rightType;
			}

			// Проверка на NULL присваивание
			if(!leftType.isReferenceType() && VarType.NULL==rightType) {
				markError("Cannot assign null to non-reference type: " + leftType);
				result = false;
			}

			if(result) {
				if(!isCompatibleWith(scope, leftType, rightType)) {
					if(VarType.FIXED==type && rightExpr instanceof LiteralExpression && rightType.isInteger()) {
						long num = ((LiteralExpression)rightExpr).getNumValue();
						if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
							markError("Type mismatch: cannot assign " + rightType + " to " + leftType);
							result = false;
						}
					}
					else {
						markError("Type mismatch: cannot assign " + rightType + " to " + leftType);
						result = false;
					}
				}
			}
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	//TODO codeGen должен вызываться для обоих операндов
	@Override
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		excs.setSourcePosition(sp);
		
		if(leftExpr.equals(rightExpr)) {
		}
		else {
			//CGScope cgs = null == parent ? cgScope : parent;

			// Для всех выражений должны быть удовлетворены зависимости
			depCodeGen(cg, leftExpr.getSymbol(), scope, excs);
			depCodeGen(cg, rightExpr.getSymbol(), scope, excs);

			// Похоже для каждого выражения с двумя операторами должен быть сброшен размер аккумулятора
			cg._getAcc().setBytes(0); // Сброс размера аккумулятора

			if(leftType.isReferenceType() && leftExpr instanceof VarFieldExpression) {
				boolean saveAccRequired = true;
				 //TODO расширить список - не нужно сохранять аккумулятор для результатов которые точно не будут в аккумуляторе
				if(rightExpr instanceof VarFieldExpression) {
					if(CGCells.Type.ACC!=((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells().getType()) {
						saveAccRequired = false;
					}
				}
				if(saveAccRequired) {
					cg.pushAccBE(cgScope, cg.getRefSize(), true);
				}
				if(type.isClassType() && !type.isEnum()) {
					cg.updateClassRefCount(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), false);
				}
				else if(type.isArray()) {
					cg.updateArrRefCount(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), false);
				}
				if(saveAccRequired) {
					cg.popAccBE(cgScope, cg.getRefSize());
				}
			}

			VarType castType = cg._getAcc().pullCast();
			// Сравнение оперирует с условными переходами, а присваивание ждет значение в аккумуляторе - обрабатываем отдельно
			if(VarType.BOOLEAN==leftType && rightExpr instanceof ComparisonExpression) {
				CGBranch branch = new CGBranch();
				rightExpr.getCGScope().setBranch(branch);
				rightExpr.codeGen(cg, true, excs);

				CGLabelScope endScope = new CGLabelScope(null, CGScope.genId(), LabelNames.COMPARE_END, true);
				cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, new CGCells(1), rightType, castType);
				cg.jump(cgScope, endScope);//cg.jump(cgScope, branch.getEnd());
				cgScope.append(branch.getEnd());
				cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, new CGCells(0), rightType, castType);
				cgScope.append(endScope);

				releaseVars(cg);
				return null;
			}

			boolean optimized = false;
			try {
				if(leftExpr instanceof ArrayExpression && rightExpr instanceof ArrayExpression) {
					rightExpr.codeGen(cg, true, excs);
					// Левое выражение не возвращает аккумулятор, но в нем могут быть выражения которые могут испортить аккумулятор (arr[i+1] = method();)
					if(CodegenResult.ACCUM_USED==leftExpr.codeGen(cg, false, excs)) {
						cg.pushAccBE(rightExpr.getCGScope(), -1==rightType.getSize() ? cg.getRefSize() : rightType.getSize(), false);
						cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ARRAY), leftType, new CGCells(CGCells.Type.STACK), rightType, castType);
					}
					else {
						cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ARRAY), leftType, new CGCells(CGCells.Type.ACC), rightType, castType);
					}
					optimized = true;
				}
				else if(null!=rightExpr.getSymbol() && null!=rightExpr.getSymbol().getCGScope() &&
						rightExpr.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells()) {

					rightExpr.codeGen(cg, false, excs);
					leftExpr.codeGen(cg, false, excs);
					cg.cellsToCells(cgScope,
									((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType,
									((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType,
									castType);
					optimized = true;
				}
	/*			else if (rightExpr instanceof MethodCallExpression && rightExpr.getType().isBoolean() && (Operator.ROL==operator || Operator.ROR==operator)) {
					leftExpr.codeGen(cg, false, excs);
					Object genResult = rightExpr.codeGen(cg, true, excs);
					if(CodegenResult.RESULT_IN_FLAG==genResult) {
						cg.cellsOpCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, operator, new CGCells(CGCells.Type.FLAG), rightType);
						optimized = true;
					}
	//				else if(CodegenResult.RESULT_IN_ACCUM==genResult) {
	//				}
				}*/
				else if(rightExpr instanceof LiteralExpression) {
					if(VarType.NULL==rightType) { // В левой части reference type (гарантирует postAnalyze)
						leftExpr.codeGen(cg, false, excs);
						CGCells rightCells = new CGCells((long)0);
						if(leftExpr instanceof VarFieldExpression) {
							// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
							cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, rightCells, null, castType);
						}
						else if(leftExpr instanceof ArrayExpression) {
//							// Декремент счетчика ссылок
//							cg.updateArrRefCount(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), false, !isView);
							// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
							cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, rightCells, null, castType);
						}
						else {
							throw new CompileException("Unexpected expression:" + leftExpr);
						}
					}
					else {
						rightExpr.codeGen(cg, false, excs);
						leftExpr.codeGen(cg, false, excs);

						CGCells rightCells = new CGCells(	rightType.isFixedPoint() ?	((LiteralExpression)rightExpr).getFixedValue() :
																						((LiteralExpression)rightExpr).getNumValue());
						cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, rightCells, rightType, castType);
					}
					optimized = true;
				}
				else if(rightExpr instanceof VarFieldExpression && null!=rightExpr.getSymbol() && null!=rightExpr.getSymbol().getCGScope()) {
					rightExpr.codeGen(cg, false, excs);
					if(CodegenResult.ACCUM_USED==leftExpr.codeGen(cg, false, excs) ) {
						cg.pushAccBE(rightExpr.getCGScope(), -1==rightType.getSize() ? cg.getRefSize() : rightType.getSize(), false);
						cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, new CGCells(CGCells.Type.STACK),
										rightType, castType);
					}
					else {
						cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType,
										((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType, castType);
					}
				}
				else {
					Object genResult = rightExpr.codeGen(cg, true, excs);
					if((Operator.ROL==operator || Operator.ROR==operator) && CodegenResult.RESULT_IN_FLAG==genResult ) {
						leftExpr.codeGen(cg, false, excs);
						cg.cellsAction(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, operator,
										new CGCells(CGCells.Type.FLAG), rightType, castType, excs);
						optimized = true;
					}
					else if(CodegenResult.RESULT_IN_ACCUM==genResult) {
						if(CodegenResult.ACCUM_USED==leftExpr.codeGen(cg, false, excs) ) {
							cg.pushAccBE(rightExpr.getCGScope(), -1==rightType.getSize() ? cg.getRefSize() : rightType.getSize(), false);
							cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, new CGCells(CGCells.Type.STACK),
											rightType, castType);
						}
						else {
							cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, new CGCells(CGCells.Type.ACC),
											rightType, castType);
						}
						optimized = true;
					}
				}
			}
			catch(Exception ex) {
				ex.printStackTrace();
				System.out.println("TODO:" + ex.getMessage());
			}

			if(!optimized) {
	/*			try { // Попытка оптимизировать блоки типа mov r16,r20; eor r16,r21; mov r20,r16 - пока нет понимания, чистые эксперименты
					CGCells cells1 = ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells();
					if(rightExpr instanceof ArithmeticExpression) {
						ArithmeticExpression ae = (ArithmeticExpression)rightExpr;
						if(ae.getLeft() == leftExpr) {
							if(ae.getRight() instanceof LiteralExpression) {
								LiteralExpression le = (LiteralExpression)ae.getRight();
								boolean isFixed = leftExpr.getType().isFixedPoint() || le.getType().isFixedPoint();						
								cg.constAction(cgScope, cells1, ae.getOperator(), isFixed ? le.getFixedValue() : le.getNumValue(), isFixed, excs);
								return null;
							}
							else {
								CGCells cells2 = ((CGCellsScope)ae.getRight().getSymbol().getCGScope()).getCells();
								cg.cellsAction(cgScope, cells1, ae.getOperator(), cells2, leftType.isFixedPoint(), ae.getRight().getType().isFixedPoint(), excs);
								return null;
							}
						}
					}
				}
				catch(Exception ex) { 
					System.out.println("TODO:" + ex.getMessage());
				}
	*/
				//================ VarFieldExpression ================================
				if(leftExpr instanceof VarFieldExpression) {
					VarFieldExpression leftVfe = (VarFieldExpression)leftExpr;
					leftExpr.codeGen(cg, false, excs);
					CGCellsScope leftCScope = (CGCellsScope)leftVfe.getSymbol().getCGScope();

					//================ VarFieldExpression ==== NULL ================================
					if(VarType.NULL==rightType) { // В левой части reference type (гарантирует postAnalyze)
						// Сохраням HEAP индексный регистр
						cg.pushHeapReg(cgScope);
						// Декремент счетчика ссылок
						cg.updateClassRefCount(cgScope, leftCScope.getCells(), false);
						// Восстанавливаем HEAP индексный регистр
						cg.popHeapReg(cgScope);
						// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
						cg.cellsToCells(cgScope, leftCScope.getCells(), leftType, new CGCells((long)0), null, castType);
					}
					//================ VarFieldExpression ==== VAR/FIELD ================================
					else if(rightExpr instanceof VarFieldExpression) {
						VarFieldExpression rightVfe = (VarFieldExpression)rightExpr;
						CGCellsScope rightCScope = (CGCellsScope)rightExpr.getSymbol().getCGScope();				
						if(CodegenResult.RESULT_IN_ACCUM!=rightVfe.codeGen(cg, true, excs)) {
							cg.cellsToCells(cgScope, new CGCells(CGCells.Type.ACC), type, ((CGCellsScope)rightCScope).getCells(), null, castType);
						}
						cg.cellsToCells(cgScope, leftCScope.getCells(), leftType, new CGCells(CGCells.Type.ACC), null, castType);
					}
					//================ VarFieldExpression ==== ARRAY ================================
					else if(rightExpr instanceof ArrayExpression) {
						ArrayExpression ae = (ArrayExpression)rightExpr;
						if(CodegenResult.RESULT_IN_ACCUM!=ae.codeGen(cg, true, excs)) throw new CompileException("Accum not used for operand:" + rightExpr);
						cg.cellsToCells(cgScope, leftCScope.getCells(), leftType, new CGCells(CGCells.Type.ACC), null, castType);
						//cg.accToCells(cgScope, leftType, leftCScope);
					}
					//================ VarFieldExpression ==== LITERAL ================================
					else if(rightExpr instanceof LiteralExpression) {
						LiteralExpression le = (LiteralExpression)rightExpr;
						cg.cellsToCells(cgScope, leftCScope.getCells(), leftType,
										new CGCells(leftType.isFixedPoint() ? le.getFixedValue() : le.getNumValue()), null, castType);
					}
					//================ VarFieldExpression ==== UNARY ================================
					else if(rightExpr instanceof UnaryExpression) {
						UnaryExpression ue = (UnaryExpression)rightExpr;

						boolean justInvertVar = false;
						if(ue.getOperand() instanceof VarFieldExpression && Operator.NOT==ue.getOperator()) {
							if(leftVfe.isSameVarFiled((VarFieldExpression)ue.getOperand())) {
								justInvertVar = true;
							}
						}

						if(justInvertVar) {
							rightExpr.codeGen(cg, false, excs);
						}
						else {
							if(CodegenResult.RESULT_IN_ACCUM==rightExpr.codeGen(cg, true, excs)) {
								cg.cellsToCells(cgScope, leftCScope.getCells(), leftType, new CGCells(CGCells.Type.ACC), null, castType);
							}
						}
					}
					//================ VarFieldExpression ==== OTHER ================================
					else {
						if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, true, excs)) {
							throw new CompileException("Accum not used for operand:" + rightExpr);
						}
						else {
							cg.cellsToCells(cgScope, leftCScope.getCells(), leftType, new CGCells(CGCells.Type.ACC), null, castType);
						}
					}
				}
				//================ ArrayExpression ================================
				else {
					
					ArrayExpression leftAe = (ArrayExpression)leftExpr;
					CGCellsScope leftCScope = (CGCellsScope)leftAe.getSymbol().getCGScope();

					//================ ArrayExpression ==== NULL ================================
					if(VarType.NULL==rightType) { // В левой части reference type (гарантирует postAnalyze)
						leftExpr.codeGen(cg, false, excs);
						// Декремент счетчика ссылок
						cg.updateArrRefCount(cgScope, leftCScope.getCells(), false);
						// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
						cg.cellsToCells(cgScope, leftCScope.getCells(), leftType, new CGCells(0), null, castType);
					}
					//================ ArrayExpression ==== ARRAY ================================
					else if(rightExpr instanceof ArrayExpression) {
						if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, true, excs)) {
							throw new CompileException("Accum not used for operand:" + rightExpr);
						}
						leftExpr.codeGen(cg, false, excs);
						cg.accToArr(cgScope, (CGArrCells)leftCScope.getCells()); //TODO castType?
					}
					//================ ArrayExpression ==== LITERAL ================================
					else if(rightExpr instanceof LiteralExpression) {
						LiteralExpression le = (LiteralExpression)rightExpr;
						leftExpr.codeGen(cg, false, excs);
						cg.cellsToCells(cgScope, leftCScope.getCells(), leftType,
										new CGCells(rightType.isFixedPoint() ? le.getFixedValue() : le.getNumValue()), null, castType);
					}
					//================ ArrayExpression ==== OTHER ================================
					else {
						optimized = false;
						if(!optimized) {
							if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, true, excs)) {
								throw new CompileException("Accum not used for operand:" + rightExpr);
							}
							cg.pushAccBE(rightExpr.getCGScope(), (rightType.isReferenceType() ? cg.getRefSize() : rightType.getSize()), false);
							leftExpr.codeGen(cg, false, excs);
							cg.popAccBE(leftExpr.getCGScope(), (rightType.isReferenceType() ? cg.getRefSize() : rightType.getSize()));
							cg.cellsToCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType,
											new CGCells(CGCells.Type.ACC), null, castType);
						}
					}
				}
			}

			// Если в правой части ссылка, то нужно увеличить счетчик ссылок
			//TODO добавить в оптимизатор проверку на рядом стоящие инкремент и декремент ссылки одного объекта
			if(rightType.isReferenceType() && rightExpr instanceof VarFieldExpression) {
				if(type.isClassType() && !type.isEnum()) {
					cg.updateClassRefCount(cgScope, ((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), true);
				}
				else if(type.isArray()) {
					cg.updateArrRefCount(cgScope, ((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), true);
				}
			}
		}
		
		releaseVars(cg);
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(leftExpr, rightExpr);
	}

	@Override
	public String toString() {
		return leftExpr + ", " + operator + ", " + rightExpr;
	}
	
	@Override
	public String getFullInfo() {
		return getClass().getSimpleName() + "  " + toString();
	}
}