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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGClassScope;
import ru.vm5277.common.cg.scopes.CGConditionScope;
import ru.vm5277.common.cg.scopes.CGExpressionScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.ClassScope;
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
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			cgScope = cg.enterExpression();
			// Анализ операндов
			if (!leftExpr.postAnalyze(scope, cg) || !rightExpr.postAnalyze(scope, cg)) {
				cg.leaveExpression();
				return false;
			}

			leftType = leftExpr.getType(scope);
			rightType = rightExpr.getType(scope);

			if(operator.isAssignment()) {
				if(leftExpr instanceof VarFieldExpression) {
					VarFieldExpression varExpr = (VarFieldExpression) leftExpr;
					Symbol symbol = scope.resolveSymbol(varExpr.getValue());
					if(null != symbol && symbol.isFinal()) {
						markError("Cannot assign to final variable: " + varExpr.getValue());
						cg.leaveExpression();
						return false;
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
						double result = 0;
						
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
						long result = 0;

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
		catch(CompileException e) {markError(e); cg.leaveExpression(); return false;}

		cg.leaveExpression();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean accumStore) throws Exception {
		return codeGen(cg, false, false, accumStore);
	}
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean accumStore) throws Exception {
		// TODO костыль. Похоже нужно управлять областью видимости универсально, а не точечно
		// Необходимо, чтобы код зависимостей формировался не там, где он находится в древе AST, а там, где он необходим, что оптимально для выделения регистров
		CGScope oldCGScope = cg.setScope(cgScope); //В таком виде верный порядок использования регистров
		
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
		CGConditionScope condScope = ((CGExpressionScope)cgScope).getConditionScope();
		if(null != condScope) {
			condScope.mustElseJump(opOr ^ isInvert);
		}

		Operator op = (operator.isAssignment() ? operator.toArithmetic() : operator);
		if(operator.isLogical()) {
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				if(le.isBoolean()) {
					boolean value = isInvert ? !(Boolean)le.getValue() : (Boolean)le.getValue();
					if((Operator.OR == operator && value) || (Operator.AND == operator && !value)) {
						cg.setScope(oldCGScope);
						return value ? CodegenResult.TRUE : CodegenResult.FALSE;
					}
				}
			}
			if(expr2 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr2;
				if(le.isBoolean()) {
					boolean value = isInvert ? !(Boolean)le.getValue() : (Boolean)le.getValue();
					if((Operator.OR == operator && value) || (Operator.AND == operator && !value)) {
						cg.setScope(oldCGScope);
						return value ? CodegenResult.TRUE : CodegenResult.FALSE;
					}
				}
			}
			
			if(expr1 instanceof BinaryExpression) ((BinaryExpression)expr1).codeGen(cg, isInvert, Operator.OR == operator, false);
			else if(expr1 instanceof UnaryExpression) {
				((UnaryExpression)expr1).codeGen(cg, isInvert, false);
			}
			else expr1.codeGen(cg, false);
			
			if(expr2 instanceof BinaryExpression) ((BinaryExpression)expr2).codeGen(cg, isInvert, Operator.OR == operator, false);
			else if(expr2 instanceof UnaryExpression) {
				((UnaryExpression)expr2).codeGen(cg, isInvert, false);
			}
			else expr2.codeGen(cg, false);
		}
		// Для присваивания всегда вычисляем правое выражение первым
		else if (operator.isAssignment()) {
			// Не строим код для leftExpr(он разместит значение в acc), а нас интересует знaчение в cells(только выполняем зависимость)
			depCodeGen(cg, leftExpr.getSymbol()); //TODO использовать метод leftExpr.codeGen(cg, false);
			// Затем обрабатываем левое выражение (куда записываем результат)
			if (leftExpr instanceof VarFieldExpression || leftExpr instanceof FieldAccessExpression) {
				CGCellsScope cScope = (CGCellsScope)leftExpr.getSymbol().getCGScope();
				if(VarType.NULL == rightType) {
					if(null == op) {
						int offset = (cScope instanceof CGVarScope ?	((CGVarScope)cScope).getStackOffset() :
																		((CGClassScope)((CGFieldScope)cScope).getParent()).getHeapHeaderSize());
						cg.constToCells(cgScope, offset, 0x00, cScope.getCells());
						cg.updateRefCount(cgScope, offset, cScope.getCells(), false);
					}
					else {
						throw new CompileException("Invalid assignment: cannot use '" + operator.getSymbol() + "' with null value");
					}
				}
				else {
					if (CodegenResult.RESULT_IN_ACCUM != rightExpr.codeGen(cg)) {
						throw new CompileException("Accum not used for operand:" + rightExpr);
					}
					cg.accToCells(cgScope, cScope);
				}
			}
			else {
				throw new CompileException("Invalid left operand for assignment: " + leftExpr);
			}
		} // Если expr2 переменная или поле(т.е. содержит cells)
		else if(expr2 instanceof VarFieldExpression) {
			CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
			//TODO Вероятно не совсем корректная модель. Условие ниже 'expr2 instanceof LiteralExpression' вообще возможно?
			if(expr1 instanceof LiteralExpression) {
				if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg)) {
					cg.cellsToAcc(cgScope, cScope);
				}
				cg.constCond(cgScope, op, ((LiteralExpression)expr1).getNumValue(), isInvert, opOr, condScope);
			}
			else {
				// Не строим код для expr2(он разместит значение в acc), а нас интересует знaчение в cells(только выполняем зависимость)
				depCodeGen(cg, expr2.getSymbol());
				// Строим код для expr1(результат в аккумуляторе)
				if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg)) {
					// Явно не доработанный код, выражение всегда должно помещать результат в аккумулятор
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				if(null != op) {
					if(cScope instanceof CGVarScope) {
						cg.cellsAction(cgScope, ((CGVarScope)cScope).getStackOffset(), cScope.getCells(), op);
					}
					else {
						cg.cellsAction(cgScope, ((CGClassScope)((CGFieldScope)cScope).getParent()).getHeapHeaderSize(), cScope.getCells(), op);
					}
				}
			}
			if(operator.isAssignment()) {
				cg.accToCells(cgScope, cScope);
			}
		}
		else if(expr2 instanceof LiteralExpression) {
			// Это константа, код не стоим, заивисимости нет
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg)) {
				// Явно не доработанный код, выражение всегда должно помещать результат в аккумулятор
				throw new CompileException("Accum not used for operand:" + expr1);
			}
			if(null != op) {
				if(op.isComparison()) {
					cg.constCond(cgScope, op, ((LiteralExpression)expr2).getNumValue(), isInvert, opOr, condScope);
				}
				else {
					cg.constAction(cgScope, op, ((LiteralExpression)expr2).getNumValue());
				}
			}
			if(operator.isAssignment()) {
				CGCellsScope cScope = (CGCellsScope)expr1.getSymbol().getCGScope();
				if(Operator.ASSIGN == operator) {
					if(cScope instanceof CGVarScope) {
						CGVarScope vScope = (CGVarScope)cScope;
						cg.constToCells(cgScope, vScope.getStackOffset(), ((LiteralExpression)expr2).getNumValue(), vScope.getCells());
					}
					else {
						CGFieldScope fScope = (CGFieldScope)cScope;
						cg.constToCells(cgScope, ((CGClassScope)fScope.getParent()).getHeapHeaderSize(), ((LiteralExpression)expr2).getNumValue(),
										fScope.getCells());
					}
				}
				else {
					cg.accToCells(cgScope, cScope);
				}
			}
		}
		else {
			//CastExpression, BinaryExpression и другие
			if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg)) {
				// Явно не доработанный код, выражение всегда должно помещать результат в аккумулятор
				throw new CompileException("Accum not used for operand:" + expr2);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Код отстроен и результат может быть только в аккумуляторе, сохраняем его
			// TODO необходимо проверить!
			// Если в expr1 не BinaryExpression, то сохранять аккумулятор не нужно?
			if(expr1 instanceof BinaryExpression) cg.pushAccBE(cgScope, size);

			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg)) {
				// Явно не доработанный код, выражение всегда должно помещать результат в аккумулятор
				throw new CompileException("Accum not used for operand:" + expr1);
			}


			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			if(null != op) {
				cg.cellsAction(cgScope, 0, new CGCells(CGCells.Type.STACK, size), op);
			}
			if(operator.isAssignment()) {
				// TODO
				cg.accToCells(cgScope, (CGCellsScope)expr1.getSymbol().getCGScope());
			}
		}
		cg.setScope(oldCGScope);
		return CodegenResult.RESULT_IN_ACCUM;
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