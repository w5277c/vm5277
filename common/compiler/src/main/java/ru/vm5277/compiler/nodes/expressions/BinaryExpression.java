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
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGBranchScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Main;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.compiler.semantic.InterfaceScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class BinaryExpression extends ExpressionNode {
    private	final	ExpressionNode	leftExpr;
    private	final	Operator		operator;
    private	final	ExpressionNode	rightExpr;
	private			VarType			leftType;
	private			VarType			rightType;
	private			boolean			isUsed		= false;
	
    public BinaryExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode left, Operator operator, ExpressionNode right) {
        super(tb, mc);
        
		this.leftExpr = left;
        this.operator = operator;
        this.rightExpr = right;
    }
    
	public ExpressionNode getLeft() {
		return leftExpr;
	}
	
	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getRight() {
		return rightExpr;
	}

	@Override
	public VarType getType(Scope scope) throws CompileException {
		// Получаем типы операндов
		leftType = leftExpr.getType(scope);
		rightType = rightExpr.getType(scope);

		if (Operator.PLUS == operator && VarType.CSTR == leftType) {
			return VarType.CSTR;
		}
		
		// Определяем тип результата операции
		if (operator.isComparison()) {
			return VarType.BOOL; // Результат сравнения - всегда boolean
		}
		
		if (operator.isLogical()) {
			return VarType.BOOL; // Логические операции - boolean
		}
		
		// Если один из операндов FIXED, результат FIXED
		if (leftType == VarType.FIXED || rightType == VarType.FIXED) {
			return VarType.FIXED;
		}
		
		// Для арифметических операций возвращаем более широкий тип
		return leftType.getSize() >= rightType.getSize() ? leftType : rightType;
	}
	
	@Override
	public boolean preAnalyze() {
		// Проверка наличия операндов
		if (null == leftExpr) {
			markError("Left operand is missing in binary expression");
			return false;
		}
		if (null == rightExpr) {
			markError("Right operand is missing in binary expression");
			return false;
		}
		
		// Рекурсивный анализ операндов
		if (!leftExpr.preAnalyze()) {
			return false; // Ошибка уже помечена в left
		}
		if (!rightExpr.preAnalyze()) {
			return false; // Ошибка уже помечена в right
		}

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (!leftExpr.declare(scope)) {
			return false;
		}
		if (!rightExpr.declare(scope)) {
			return false;
		}
		
		if(Operator.ASSIGN == operator && leftExpr instanceof VarFieldExpression && rightExpr instanceof VarFieldExpression) {
			if(((VarFieldExpression)leftExpr).getValue().equals(((VarFieldExpression)rightExpr).getValue())) {
				markWarning("Self-assignment for '" + ((VarFieldExpression)rightExpr).getValue() + "' has no effect");
				disable();
			}
		}
		
		if(operator.isAssignment() && leftExpr instanceof VarFieldExpression) {
			VarFieldExpression vfe = (VarFieldExpression)leftExpr;
			Symbol vfs = scope.resolveSymbol(vfe.getValue());
			if(null != vfs) {
				vfs.setReassigned();
			}
		}
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		cgScope = cg.enterExpression(toString());
		
		try {
			// Анализ операндов
			if (!leftExpr.postAnalyze(scope, cg) || !rightExpr.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}

			leftType = leftExpr.getType(scope);
			rightType = rightExpr.getType(scope);

			// Проверка на недопустимые операции для boolean типов
			if (leftType.isBoolean() || rightType.isBoolean()) {
				// Запрещаем арифметические операции с boolean
				if (operator.isArithmetic()) {
					markError("Arithmetic operations cannot be applied to boolean operands");
					result = false;
				}

				// Запрещаем операции сравнения (кроме равенства/неравенства) для boolean
				if (operator.isComparison() && Operator.EQ!=operator && Operator.NEQ!=operator) {
					markError("Comparison operations (other than == and !=) cannot be applied to boolean operands");
					result = false;
				}

				// Запрещаем побитовые операции с boolean
				if (operator.isBitwise()) {
					markError("Bitwise operations cannot be applied to boolean operands");
					result = false;
				}
			}
			
			if(operator.isAssignment()) {
				if(	leftExpr instanceof ArrayExpression &&
					((ArrayExpression)leftExpr).getDepth() != ((ArrayExpression)leftExpr).getTargetExpr().getType(scope).getArrayDepth()) {

					markError("Array assignment dimension mismatch");
					result = false;
				}
				else if(leftExpr instanceof VarFieldExpression) {
					VarFieldExpression varExpr = (VarFieldExpression) leftExpr;
					Symbol symbol = scope.resolveSymbol(varExpr.getValue());
					if(null != symbol && symbol.isFinal()) {
						markError("Cannot assign to final variable: " + varExpr.getValue());
						cg.leaveExpression();
						return false;
					}
					if(rightExpr instanceof UnaryExpression) {
						Operator unaryOp = ((UnaryExpression)rightExpr).getOperator();
						if(Operator.MINUS == unaryOp && !varExpr.getType(scope).isFixedPoint()) {
							if(AssemblerInterface.STRICT_STRONG == Main.getStrictLevel()) {
								markError("Unary " + ((UnaryExpression)rightExpr).getOperator() + " requires fixed type");
								cg.leaveExpression();
								return false;
							}
							else if(AssemblerInterface.STRICT_LIGHT == Main.getStrictLevel()) {
								markWarning("Unary " + ((UnaryExpression)rightExpr).getOperator() + " requires fixed type");
							}
						}
						else if(Operator.PRE_INC==unaryOp || Operator.PRE_DEC==unaryOp || Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
							UnaryExpression ue = (UnaryExpression)rightExpr;
							if(ue.getOperand() instanceof VarFieldExpression && varExpr.getValue().equals(((VarFieldExpression)ue.getOperand()).getValue())) {
								if(AssemblerInterface.STRICT_STRONG == Main.getStrictLevel()) {
									if(Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
										markError("Redudnand statement has no practical effect");
									}
									else {
										markError("Redudnand statement");
									}
									result = false;
								}
								else if(AssemblerInterface.STRICT_LIGHT == Main.getStrictLevel()) {
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
				else if (leftExpr instanceof FieldAccessExpression) {
					// Для полей классов проверяем final (если нужно)
					FieldAccessExpression memberExpr = (FieldAccessExpression) leftExpr;
					ExpressionNode target = memberExpr.getTarget();
					String fieldName = memberExpr.getFieldName();

					// Получаем тип объекта
					VarType targetType = target.getType(scope);

					if (targetType.isClassType()) {
						// Ищем класс в scope
						InterfaceScope iScope = scope.getThis().resolveScope(targetType.getClassName());
						if (null != iScope && iScope instanceof ClassScope) {
							Symbol field = ((ClassScope)iScope).getFields().get(fieldName);
							if (null != field && field.isFinal()) {
								markError("Cannot assign to final field: " + targetType.getClassName() + "." + fieldName);
								cg.leaveExpression();
								return false;
							}
						}
					}
	            }
			}

			// Специфичные проверки операторов
			if (operator.isLogical()) {
				if (!leftType.isBoolean() || !rightType.isBoolean()) {
					markError("Logical operators require boolean operands");
					cg.leaveExpression();
					return false;
				}
			}
			else if (operator.isBitwise()) {
				if (!leftType.isInteger() || !rightType.isInteger()) {
					markError("Bitwise operators require integer operands");
					cg.leaveExpression();
					return false;
				}
			}
			else if(Operator.MOD == operator && VarType.FIXED==rightType) {
				markError("Modulo operator % cannot be applied to 'fixed'");
				result = false;
			}
			else if ((Operator.DIV == operator || Operator.MOD == operator)) { // Проверка деления на ноль (универсальная для всех типов)
				if (rightExpr instanceof LiteralExpression) {
					Object val = ((LiteralExpression)rightExpr).getValue();
					boolean isZero = false;

					if (val instanceof Double) isZero = (Double)val == 0.0;
					else if (val instanceof Number) isZero = ((Number)val).longValue() == 0L;

					if (isZero) {
						markError("Division by zero");
						cg.leaveExpression();
						return false;
					}
				}
			}
			else if (operator.isArithmetic()) { // Проверки для арифметических операций
				// Проверки для численных типов
				if (leftExpr instanceof LiteralExpression && rightExpr instanceof LiteralExpression) {
					LiteralExpression leftLE = (LiteralExpression)leftExpr;
					LiteralExpression rightLE = (LiteralExpression)rightExpr;

					if(leftType.isFixedPoint() || rightType.isFixedPoint()) {
						double leftVal = leftLE.getValue() instanceof Double ? ((Double)leftLE.getValue()) : ((Number)leftLE.getValue()).doubleValue();
						double rightVal = rightLE.getValue() instanceof Double ? ((Double)rightLE.getValue()) : ((Number)rightLE.getValue()).doubleValue();
						
						try	{					
							switch (operator) {
								case PLUS: leftType.checkRange(leftVal + rightVal); break;
								case MINUS: leftType.checkRange(leftVal - rightVal); break;
								case MULT: leftType.checkRange(leftVal * rightVal); break;
								case DIV: leftType.checkRange(leftVal / rightVal); break;
							}
						}
						catch(CompileException e) {
							markError(e);
							cg.leaveExpression();
							return false;
						}
					}
					else {
						long leftVal = ((Number)leftLE.getValue()).longValue();
						long rightVal = ((Number)rightLE.getValue()).longValue();

						try {
							switch (operator) {
								case PLUS: leftType.checkRange(leftVal + rightVal); break;
								case MINUS: leftType.checkRange(leftVal - rightVal); break;
								case MULT: leftType.checkRange(leftVal * rightVal); break;
								case DIV: leftType.checkRange(leftVal / rightVal); break;
								case MOD: leftType.checkRange(leftVal % rightVal); break;
							}
						}
						catch(CompileException e) {
							markError(e);
							cg.leaveExpression();
							return false;
						}
					}
				}
				
				// Особые проверки для fixed-point
				if (leftType == VarType.FIXED || rightType == VarType.FIXED) {
					if (operator == Operator.MOD) {
						markError("Modulo operation is not supported for FIXED type");
						cg.leaveExpression();
						return false;
					}
				}
			}
		}
		catch(CompileException e) {
			markError(e);
			result = false;
		}

		cg.leaveExpression();
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		return codeGen(cg, parent, false, false, toAccum);
	}
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum) throws Exception {
		if(disabled) return null;
		
		CodegenResult result = CodegenResult.RESULT_IN_ACCUM;

		CGScope cgs = null == parent ? cgScope : parent;
		
		int leftSize = leftType.getSize();
		int rightSize = rightType.getSize();
		int maxSize = Math.max(leftSize, rightSize);
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Оптимизация порядка для коммутативных операций
		if(operator.isCommutative()) {
			if(leftExpr instanceof VarFieldExpression || leftExpr instanceof LiteralExpression) {
				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}
		
		Operator op = (operator.isAssignment() ? operator.toArithmetic() : operator);
		CGScope brScope = null;
		if(operator.isLogical() || (null!=op && op.isComparison())) {
			brScope = cgs;
			while(null != brScope && !(brScope instanceof CGBranchScope)) {
				brScope = brScope.getParent();
			}
		}
//		if(null != brScope) {
//			((CGBranchScope)brScope).mustElseJump(opOr ^ isInvert);
//		}

		if(operator.isLogical()) { // AND или OR
			if(null == brScope) {
				// Вычисление результата в аккумуляторе (без условных переходов)
				// Генерируем код для левого операнда
				if (CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				// Сохраняем результат левого операнда
				//TODO cg.pushAccBE(expr1.getCGScope(), 1); // boolean занимает 1 байт
				cg.pushAccBE(cgs, 1); // boolean занимает 1 байт

				// Генерируем код для правого операнда
				if (CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}
				// Выполняем логическую операцию со значением в стеке
				cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, 1), operator, false);
			}
			else {
				//TODO Генерирует не оптимальное количество переходов, оптимизатор решит эту задачу, но лучше оптимизировать сразу
				if(expr1 instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)expr1;
					if(le.isBoolean()) {
						boolean value = isInvert ? !(Boolean)le.getValue() : (Boolean)le.getValue();
						if((Operator.OR == operator && value) || (Operator.AND == operator && !value)) {
							return value ? CodegenResult.TRUE : CodegenResult.FALSE;
						}
					}
				}
				if(expr2 instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)expr2;
					if(le.isBoolean()) {
						boolean value = isInvert ? !(Boolean)le.getValue() : (Boolean)le.getValue();
						if((Operator.OR == operator && value) || (Operator.AND == operator && !value)) {
							return value ? CodegenResult.TRUE : CodegenResult.FALSE;
						}
					}
				}

				if(opOr ^ (Operator.OR == operator)) {
					((CGBranchScope)brScope).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_END, true)); //Конец логического блока OR или AND
				}

				if(expr1 instanceof BinaryExpression) {
					if(CodegenResult.RESULT_IN_ACCUM != ((BinaryExpression)expr1).codeGen(cg, cgs, isInvert, Operator.OR == operator, false)) {
						result = null;
					}
				}
				else if(expr1 instanceof VarFieldExpression) {
					((VarFieldExpression)expr1).codeGen(cg, isInvert, Operator.OR == operator, (CGBranchScope)brScope);
					result = null;
				}
				else if(expr1 instanceof UnaryExpression) {
					((CGBranchScope)brScope).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_NOT_END, true));
					((UnaryExpression)expr1).codeGen(cg, null, isInvert, Operator.OR == operator, false);
					CGLabelScope lbScope = ((CGBranchScope)brScope).popEnd();
					//TODO проверить(не стандартный CGScope)
					//cg.jump(expr1.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr1.getCGScope().append(lbScope);
					cg.jump(cgs, ((CGBranchScope)brScope).getEnd());
					cgs.append(lbScope);
				}
				else {
					expr1.codeGen(cg, null, false);
				}

				if(expr2 instanceof BinaryExpression) {
					if(CodegenResult.RESULT_IN_ACCUM != ((BinaryExpression)expr2).codeGen(cg, cgs, isInvert, Operator.OR == operator, false)) {
						result = null;
					}
				}
				else if(expr2 instanceof VarFieldExpression) {
					((VarFieldExpression)expr2).codeGen(cg, isInvert, Operator.OR == operator, (CGBranchScope)brScope);
					result = null;
				}
				else if(expr2 instanceof UnaryExpression) {
					((CGBranchScope)brScope).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_NOT_END, true));
					((UnaryExpression)expr2).codeGen(cg, null, isInvert, Operator.OR == operator, false);
					CGLabelScope lbScope = ((CGBranchScope)brScope).popEnd();
					//TODO проверить(не стандартный CGScope)
					//cg.jump(expr2.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr2.getCGScope().append(lbScope);
					cg.jump(cgs, ((CGBranchScope)brScope).getEnd());
					cgs.append(lbScope);

				}
				else {
					expr2.codeGen(cg, null, false);
				}

				if(opOr ^ (Operator.OR == operator)) {
					CGLabelScope lbScope = ((CGBranchScope)brScope).popEnd();
					//cg.jump(expr2.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr2.getCGScope().append(lbScope);
					cg.jump(cgs, ((CGBranchScope)brScope).getEnd());
					cgs.append(lbScope);
				}
			}
		}

		// Для присваивания всегда вычисляем правое выражение первым
		// Обрабатываем только Operator.ASSIGN
		else if (operator.isAssignment() && null == op) {
			if (leftExpr instanceof VarFieldExpression || leftExpr instanceof FieldAccessExpression) {
				leftExpr.codeGen(cg, cgs, false);
				CGCellsScope cScope = (CGCellsScope)leftExpr.getSymbol().getCGScope();
				if(VarType.NULL == rightType) {
					if(null == op) {
						if(leftType.isArray()) {
							//TODO нужно проверить!
							boolean isView = false;
							AstHolder ah = (AstHolder)leftExpr.getSymbol();
							if(ah.getNode() instanceof InitNodeHolder) {
								InitNodeHolder inh = (InitNodeHolder)ah.getNode();
								if(inh.getInitNode() instanceof ArrayExpression) {
									isView = true;
								}
							}
							cg.updateArrRefCount(cgs, cScope.getCells(), false, isView);
						}
						else {
							cg.updateClassRefCount(cgs, cScope.getCells(), false);
						}
						// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
						cgs.append(cg.constToCells(cgs, 0x00, cScope.getCells(), false));
					}
					else {
						throw new CompileException("Invalid assignment: cannot use '" + operator.getSymbol() + "' with null value");
					}
				}
				else {
					if(rightExpr instanceof LiteralExpression) {
						LiteralExpression le = (LiteralExpression)rightExpr;
						boolean isFixed = le.isFixed() || VarType.FIXED == cScope.getType();
						cgs.append(cg.constToCells(cgs, isFixed ? le.getFixedValue() : le.getNumValue(), cScope.getCells(), isFixed));
					}
					else {
						boolean justInvertVar = false;
						if(leftExpr instanceof VarFieldExpression && rightExpr instanceof UnaryExpression) {
							VarFieldExpression vfe1 = (VarFieldExpression)leftExpr;
							UnaryExpression ue = (UnaryExpression)rightExpr;
							if(ue.getOperand() instanceof VarFieldExpression && Operator.NOT == ue.getOperator()) {
								VarFieldExpression vfe2 = (VarFieldExpression)ue.getOperand();
								if(vfe1.getValue().equals(vfe2.getValue())) {
									justInvertVar = true;
								}
							}
						}
						
						if(justInvertVar) {
							rightExpr.codeGen(cg, cgScope, false);
						}
						else {
							if(CodegenResult.RESULT_IN_ACCUM != rightExpr.codeGen(cg, null, true)) {
								//Не все унарные операции влияют на аккумулятор
								if(!(rightExpr instanceof UnaryExpression)) {
									throw new CompileException("Accum not used for operand:" + rightExpr);
								}
							}
							else {
								cgs.append(cg.accCast(rightType, leftType));
								cg.accToCells(cgs, cScope);
							}
						}
					}
				}
			}
			else if (leftExpr instanceof ArrayExpression) {
				if(rightExpr instanceof LiteralExpression) {
					// Формируем CGCells и помещаем в стек индексы(если не можем вычислить статическеий адрес)
					leftExpr.codeGen(cg, null, false);

					LiteralExpression le = (LiteralExpression)rightExpr;
					boolean isFixed = le.isFixed() || VarType.FIXED == leftType;
					cgs.append(	cg.constToCells(cgs, isFixed ? le.getFixedValue() : le.getNumValue(),
									((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), isFixed));
				}
				else if(rightExpr instanceof ArrayExpression) {
					rightExpr.codeGen(cg, cgs, true);
					//cg.arrToAcc(cgs, (CGArrCells)((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells());
					leftExpr.codeGen(cg, cgs, false);
					cgs.append(cg.accCast(rightType, leftType));
					cg.accToArr(cgs, (CGArrCells)((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells());
				}
				else {
					if (CodegenResult.RESULT_IN_ACCUM != rightExpr.codeGen(cg, cgs, true)) {
						throw new CompileException("Accum not used for operand:" + rightExpr);
					}
					//TODO в правой части хоть и указано, что не требуется результат в аккумулятор, тем не менее могут быть сложные выражения использующие аккумулятор
					//Корректно сохранять аккумулятор в этих выражениях. Нужно провести анализ так как скорее всего все немного сложнее.
					//подвыражения не знают занят ли аккумулятор, и могут избыточно его каждый раз сохранять
					int accumSize = cg.getAccumSize();
					cg.pushAccBE(cgs, accumSize);
					leftExpr.codeGen(cg, cgs, false);
					cg.popAccBE(cgs, accumSize);
					cg.accCast(rightType, leftType);
					//TODO не оптимально, лучше создать метод stackToCells, но он пока не нужен, нужно решить TODO чуть выше
					cg.accToCells(cgs, (CGCellsScope)leftExpr.getSymbol().getCGScope());
				}
			}
			else {
				throw new CompileException("Invalid left operand for assignment: " + leftExpr);
			}
		} // Если expr2 переменная или поле(т.е. содержит cells)
		else if(expr2 instanceof VarFieldExpression) {
			
			//TODO Вероятно не совсем корректная модель. Условие ниже 'expr2 instanceof LiteralExpression' вообще возможно?
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				if(op.isComparison()) {
					expr2.codeGen(cg, null, false);
					CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
					cg.constCond(cgs, cScope.getCells(), op, le.getNumValue(), isInvert, opOr, (CGBranchScope)brScope);
					result = null;
				}
				else {
					if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, null, true)) {
						throw new CompileException("Accum not used for operand:" + expr1);
					}
					
					// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
					if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
						cgs.append(cg.accCast(expr2.getType(null), expr1.getType(null)));
					}
					// Добавить проверку деления на 0
					cg.constAction(cgs, op, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed());
				}
			}
			else if(expr1 instanceof ArrayExpression) {
				//Нужно использовать аккумулятор для bool bl = bArray1[0]>bt && bt==2;
				expr1.codeGen(cg, cgs, true);
				//cg.arrToAcc(cgs, (CGArrCells)((CGCellsScope)expr1.getSymbol().getCGScope()).getCells());

				if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
				}
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				if(op.isComparison()) {
					cg.cellsCond(cgs, cScope.getCells(), op, isInvert, opOr, (CGBranchScope)brScope);
					result = null;
				}
				else {
//					if(null != op) { //TODO
					cg.cellsAction(cgs, cScope.getCells(), op, VarType.FIXED == expr2.getType(null));
//					}
				}
//				expr2.codeGen(cg, cgs, false);
//				cg.accToArr(cgs, (CGArrCells)((CGCellsScope)expr2.getSymbol().getCGScope()).getCells());
//				cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
			}
			else {
				// Не строим код для expr2(он разместит значение в acc), а нас интересует знaчение в cells(только выполняем зависимость)
				depCodeGen(cg, expr2.getSymbol());
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				// Строим код для expr1(результат в аккумуляторе)
				if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
					// Явно не доработанный код, выражение всегда должно помещать результат в аккумулятор
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
				if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
				}
				if(op.isComparison()) {
					cg.cellsCond(cgs, cScope.getCells(), op, isInvert, opOr, (CGBranchScope)brScope);
					result = null;
				}
				else {
//					if(null != op) { //TODO
					cg.cellsAction(cgs, cScope.getCells(), op, VarType.FIXED == expr2.getType(null));
//					}
				}
			}
			if(operator.isAssignment()) {
				//!!!CGScope oldScope = cg.setScope(scope);
				//TODO Можно оптимизировать? Копировать без участия аккумулятора?
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, null, true)) {
					cg.cellsToAcc(cgs, cScope);
				}
				cg.accToCells(cgs, cScope);
			}
		}
		else if(expr2 instanceof LiteralExpression) {
			// Это константа, код не стоим, заивисимости нет
			// Строим код для expr1(результат в аккумуляторе не нужен)
			if(null != op) {
				if(op.isComparison()) {
					if(expr1 instanceof BinaryExpression) {
						expr1.codeGen(cg, null, true);
						cg.constCond(	cgs, new CGCells(CGCells.Type.ACC), op, ((LiteralExpression)expr2).getNumValue(), isInvert, opOr,
										(CGBranchScope)brScope);
						result = null;
					}
					else {
						expr1.codeGen(cg, null, false);
						CGCellsScope cScope = (CGCellsScope)expr1.getSymbol().getCGScope();
						cg.constCond(cgs, cScope.getCells(), op, ((LiteralExpression)expr2).getNumValue(), isInvert, opOr, (CGBranchScope)brScope);
						result = null;
					}
				}
				else {
					if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
						if(!(expr1 instanceof ArrayExpression)) {
							throw new CompileException("Accum not used for operand:" + expr1);
						}
					}
					// Добавить проверку деления на 0
					LiteralExpression le = (LiteralExpression)expr2;
					// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
					if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
						cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
					}
					cg.constAction(cgs, op, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed());
				}
			}
			if(operator.isAssignment()) {
				//!!!CGScope oldScope = cg.setScope(scope);
				CGCellsScope cScope = (CGCellsScope)expr1.getSymbol().getCGScope();
				if(Operator.ASSIGN == operator) {
					LiteralExpression le = (LiteralExpression)expr2;
					boolean isFixed = le.isFixed() || VarType.FIXED == cScope.getType();
					cgs.append(cg.constToCells(cgs, isFixed ? le.getFixedValue() : le.getNumValue(), cScope.getCells(), isFixed));
				}
				else {
					cg.accToCells(cgs, cScope);
				}
			}
		}
		else if(expr2 instanceof ArrayExpression) {
			//TODO
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				if(op.isComparison()) {
					expr2.codeGen(cg, null, false);
					CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
					cg.constCond(cgs, cScope.getCells(), op, le.getNumValue(), isInvert, opOr, (CGBranchScope)brScope);
					result = null;
				}
				else {
					if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, null, true)) {
						throw new CompileException("Accum not used for operand:" + expr2);
					}
					//Аккумулятор уже необходимого размера, но нужно проверить на Fixed
					if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
						cgs.append(cg.accCast(expr2.getType(null), expr1.getType(null)));
					}
					//Добавить проверку деления на 0
					cg.constAction(cgs, op, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed());
				}
			}
			else if(expr1 instanceof VarFieldExpression) {
				VarFieldExpression vfe = (VarFieldExpression)expr1;
				if(op.isComparison()) {
					if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
						throw new CompileException("Accum not used for operand:" + expr1);
					}
//!!!!!!!!! Нужно или нет сохранять в стек? Ранее было:
//cg.pushAccBE(cgs, 0x02); //0x02 - размер адреса массива
//expr2.codeGen(cg, cgs, true);
//Убрал сохранение в стек, и expr2 не нужно записывать в accum
//Для проверки:	short s=0x0101;	s++; if(s<arr2[3]) {

					expr2.codeGen(cg, cgs, false);
					CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
					cg.cellsCond(cgs, cScope.getCells(), op, isInvert, opOr, (CGBranchScope)brScope);
					result = null;
				}
				else {
/*					if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, null, true)) {
						throw new CompileException("Accum not used for operand:" + expr1);
					}
					//Аккумулятор уже необходимого размера, но нужно проверить на Fixed
					if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
						cgs.append(cg.accCast(expr2.getType(null), expr1.getType(null)));
					}
					//Добавить проверку деления на 0
					cg.constAction(cgs, op, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed());*/
				}
			}
			else {

/*				//Временное решение, см. ArrayExpression.codegen, для ArrayExpression нужно указывать размещение адреса в аккумулятор
				expr2.codeGen(cg, null, false);
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				// Строим код для expr1(результат в аккумуляторе)
				if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, null, true)) {
					// Явно не доработанный код, выражение всегда должно помещать результат в аккумулятор
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
				if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
				}
				if(op.isComparison()) {
					cg.cellsCond(cgs, cScope.getCells(), op, isInvert, opOr, (CGBranchScope)brScope);
					result = null;
				}
				else {
//					if(null != op) { //TODO
					cg.cellsAction(cgs, cScope.getCells(), op, VarType.FIXED == expr2.getType(null));
//					}
				}*/


				if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}					

				// Определяем максмальный размер операнда
				int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
				// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
				if(null != op) {
					// Код отстроен и результат может быть только в аккумуляторе, сохраняем его
//					if(expr1 instanceof BinaryExpression) {
					//Необходимо как минимум для ArrayExpression(скорее всего для всех, которые здесь обрабатываются)
					cg.pushAccBE(cgs, size);
//					}
					// Строим код для expr1(результат в аккумуляторе)
					if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
						throw new CompileException("Accum not used for operand:" + expr1);
					}

					// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
					if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
						cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
					}
					cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, size), op, VarType.FIXED == expr2.getType(null));
				}				
				
				
			}
			if(operator.isAssignment()) {
				//TODO Можно оптимизировать? Копировать без участия аккумулятора?
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, null, true)) {
					cg.cellsToAcc(cgs, cScope);
				}
				cg.accToCells(cgs, cScope);
			}
		}
		else {
			//CastExpression, BinaryExpression и другие
			if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, cgs, true)) {
				throw new CompileException("Accum not used for operand:" + expr2);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			if(null != op) {
				// Код отстроен и результат может быть только в аккумуляторе, сохраняем его
//				if(expr1 instanceof BinaryExpression) {
					cg.pushAccBE(cgs, size);
//				}
				// Строим код для expr1(результат в аккумуляторе)
				if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}

				// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
				if(expr1.getType(null).isFixedPoint() ^ expr2.getType(null).isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(null), expr2.getType(null)));
				}
				cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, size), op, VarType.FIXED == expr2.getType(null));
				if(operator.isAssignment()) {
					cg.accToCells(cgs, (CGCellsScope)expr1.getSymbol().getCGScope());
				}
			}
			else {
				//TODOЧто делать если null==op?
				throw new CompileException("Not supported yet: " + expr1 + ", " + operator + ", " + expr2);
			}
		}

		return result;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(leftExpr, rightExpr);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "  " + leftExpr + ", " + operator + ", " + rightExpr;
	}
}