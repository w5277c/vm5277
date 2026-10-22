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
import java.util.Objects;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.enums.J8BException;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.CODEGEN;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.compiler.Instance;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.Scope;

public class ArithmeticExpression extends BinaryExpression {
	private boolean partOfAssignment;
    private boolean hasDivisionByZero;
	
	ArithmeticExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right, boolean partOfAssignment) {
        super(inst, tb, sp, left, op, right);
		this.partOfAssignment = partOfAssignment;
    }
    
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		this.scope = scope;
		cgScope = cg.enterExpression(parent, sp, cgScope, toString());

		// Анализ операндов
		result&=leftExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(leftExpr);
			if(null!=resolved) {
				leftExpr = resolved;
			}
		}

		result&=rightExpr.postAnalyze(scope, cg, cgScope);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(rightExpr);
			if(null!=resolved) {
				rightExpr = resolved;
			}
		}

		if(result) {
			leftType = leftExpr.getType();
			rightType = rightExpr.getType();
		}

		if(result) {
			// Запрещаем операции с reference
			if(leftType.isReferenceType() || rightType.isReferenceType()) {
				markError("Cannot perform operation with reference types");
				result = false;
			}
		}
		if(result) {
			// Запрещаем операции с NULL
			if(VarType.NULL==leftType || VarType.NULL==rightType) {
				markError("Cannot perform operation with null");
				result = false;
			}
		}
		if(result) {
			// Запрещаем операции с bool типами кроме конкатенации со строкой
			if(	(leftType.isBoolean() || rightType.isBoolean()) && (Operator.PLUS!=operator ||
				(VarType.CSTR!=leftType && VarType.CSTR!=rightType && VarType.CHAR!=leftType && VarType.CHAR!=rightType))) {

				markError("Arithmetic operations cannot be applied to bool operands");
				result = false;
			}
		}
		if(result) {
			// Запрещаем операции с CSTR
			if(Operator.PLUS!=operator && (VarType.CSTR==leftType || VarType.CSTR==rightType)) {
				markError("Only concatenation is allowed for cstr operands");
				result = false;
			}
		}

		if(result) {
			if(Operator.MOD==operator && VarType.FIXED==rightType) {
				markError("Modulo operator % cannot be applied to 'fixed'");
				result = false;
			}
		}
		
		if(result) {
			// Проверка деления на ноль (универсальная для всех типов)
			if((Operator.DIV==operator || Operator.MOD==operator)) {
				if(rightExpr instanceof LiteralExpression) {
					Object val = ((LiteralExpression)rightExpr).getValue();
					boolean isZero = false;

					if(val instanceof Double) {
						hasDivisionByZero = ((Double)val==0.0);
					}
					else if(val instanceof Number) {
						hasDivisionByZero = (((Number)val).longValue()==0L);
					}

					if(hasDivisionByZero) {
						if(StrictLevel.NONE==inst.getStrictLevel()) {
						}
						else if(StrictLevel.STRONG==inst.getStrictLevel()) {
							markError("Division by zero");
							result = false;
						}
						else {
							markWarning("Division by zero");
						}
					}
				}
			}
		}
		
		if(result) {
			// Если один из операндов FIXED, результат FIXED
			if(VarType.FIXED==leftType || VarType.FIXED==rightType) {
				type = VarType.FIXED;
			}
			else if(VarType.CSTR==leftType || VarType.CSTR==rightType) {
				type = VarType.CSTR;
			}
			else {
				int maxOperandSize = Math.max(leftType.getSize(), rightType.getSize());
				if(partOfAssignment) {
					type = leftType;
				}
				else {
					switch (operator) {
						case PLUS:
						case MULT:
						case INC:
						case PRE_INC:
						case POST_INC:
							if(0x01==maxOperandSize) {
								type = VarType.SHORT;
							}
							else {
								type = VarType.INT;
							}
							break;
						case MINUS:
						case DEC:
						case PRE_DEC:
						case POST_DEC:
						case DIV:
						case MOD:
							if(0x01==maxOperandSize) {
								type = VarType.BYTE;
							}
							else if(0x02==maxOperandSize) {
								type = VarType.SHORT;
							}
							else {
								type = VarType.INT;
							}
							break;
						case SHR:
							type = leftType;
							break;
						case BIT_AND:
							type = (leftType.getSize()<rightType.getSize() ? leftType : rightType);
							break;
						case BIT_OR:
						case BIT_XOR:
							type = (leftType.getSize()>rightType.getSize() ? leftType : rightType);
							break;
						default:
							type = VarType.INT;
					}
				}
			}
		}

		if(result) {
			// Проверки для численных типов
			// TODO константы еще не собраны, мы не можем здесь проверять, так как теперь проверяем наличие финальной переменной или свойства
/*			if(leftExpr instanceof LiteralExpression && rightExpr instanceof LiteralExpression) {
				LiteralExpression leftLE = (LiteralExpression)leftExpr;
				LiteralExpression rightLE = (LiteralExpression)rightExpr;
				
				boolean varFiledCast = (leftLE.fromVarField() | rightLE.fromVarField());
				VarType type_ = leftLE.getType().getSize() > rightLE.getType().getSize() ? leftLE.getType() : rightLE.getType();

				
				if(leftType.isFixedPoint() || rightType.isFixedPoint()) {
					double leftVal = leftLE.getValue() instanceof Double ? ((Double)leftLE.getValue()) : ((Number)leftLE.getValue()).doubleValue();
					double rightVal = rightLE.getValue() instanceof Double ? ((Double)rightLE.getValue()) : ((Number)rightLE.getValue()).doubleValue();

					try	{					
						switch (operator) {
							case PLUS: leftType.checkRange(leftVal+rightVal); break;
							case MINUS: leftType.checkRange(leftVal-rightVal); break;
							case MULT: leftType.checkRange(leftVal*rightVal); break;
							case DIV: leftType.checkRange(leftVal/rightVal); break;
						}
					}
					catch(CompileException e) {
						markError(e);
						result = false;
					}
				}
				else {
					long leftVal = ((Number)leftLE.getValue()).longValue();
					long rightVal = ((Number)rightLE.getValue()).longValue();

					try {
						switch (operator) {
							case PLUS: leftType.checkRange(leftVal+rightVal); break;
							case MINUS: leftType.checkRange(leftVal-rightVal); break;
							case MULT: leftType.checkRange(leftVal*rightVal); break;
							case DIV: leftType.checkRange(leftVal/rightVal); break;
							case MOD: leftType.checkRange(leftVal%rightVal); break;
						}
					}
					catch(CompileException e) {
						markError(e);
						result = false;
					}
				}
			}*/
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	//TODO Добавить явные случаи Array + VarField и VarField + Array
	//TODO Проверить порядок операндов в Literal + VarField
	//TODO главная проблема - порядок cgScope(в том числе и выражений) задается в postAnalyze. Мы не пожем просто по порядку записывать в cgScope, нужно
	//учитывать порядок leftExpr.cgScope и rightExpr.cgScope (cм. //================ ArrayExpression ==== OTHER ================================
	@Override
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(cgDone || disabled) return null;
		cgDone = true;

		debugAST(this, CODEGEN, true, getFullInfo());

		if(hasDivisionByZero) {
			Integer exceptionId = cg.getExcsChecker().getHandled(excs, J8BException.DivByZeroException.name());
			if(null!=exceptionId) {
				cg.getExcsChecker().makeException(cg, cgScope, excs, exceptionId);
			}
		}
		
		VarType cast = cg._getAcc().pullCast();
		// Выражения типа octet <<= 1; (где octet-byte) раскрываются в блок octet = (byte)(octet<<1);
		// Здесь мы добавляем cast, иначе кодогенератор будет использовать аккумулятор большего размера
		if(null==cast && partOfAssignment) {
			cast = leftType;
		}
		excs.setSourcePosition(sp);
		
		Object result = CodegenResult.RESULT_IN_ACCUM;
		
		//CGScope cgs = null == parent ? cgScope : parent;
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Похоже для всех выражений должны быть удовлетворены зависимости
		depCodeGen(cg, expr1.getSymbol(), scope, excs);
		depCodeGen(cg, expr2.getSymbol(), scope, excs);
		
		// Похоже для каждого выражения с двумя операторами должен быть сброшен размер аккумулятора
		cg._getAcc().setBytes(0); // Сброс размера аккумулятора
		
		// Оптимизация порядка для коммутативных операций
		if(operator.isCommutative()) {
			int leftTypeSize = (leftExpr.getType().isReferenceType() ? cg.getRefSize() : leftExpr.getType().getSize());
			int rightTypeSize = (rightExpr.getType().isReferenceType() ? cg.getRefSize() : rightExpr.getType().getSize());

/*			if(leftTypeSize<rightTypeSize) {
				//AVR не умеет выполнять сложение с константой компактно
				if(	(leftExpr instanceof LiteralExpression && PlatformType.AVR!=cg.getDevice().getPlatform().getType()) || - 
					(!(rightExpr instanceof LiteralExpression) && leftExpr instanceof VarFieldExpression)) {
				}
				else {*/
			// Для коммутативных операций VarFieldExpression лучше переносить в правую часть, так данные можно получить без использования аккумулятора,
			// а значит снижается вероятность помещения одного из операндов в стек
			// При этом нельзя допускать, чтобы размер левого операнда был меньше правого
			//TODO добавить остальные типы выражений, данные которых можно также получить без аккумулятора
			//(leftExpr instanceof LiteralExpression && leftTypeSize<=rightTypeSize) - не важно какой размер, константа всегда должна быть справа
			if(	(leftExpr instanceof VarFieldExpression && !(rightExpr instanceof VarFieldExpression) && leftTypeSize<=rightTypeSize) ||
				(leftExpr instanceof LiteralExpression) ||
				(leftTypeSize<rightTypeSize)) {
				
				expr1 = rightExpr;
				expr2 = leftExpr;
				cgScope.swap(expr1.getCGScope(), expr2.getCGScope());
			}
		}

/*		try { //Пытаемся оптимизировать, пока просто набросок
			if(	null!=rightExpr.getSymbol() && null!=rightExpr.getSymbol().getCGScope() &&
				rightExpr.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells() &&
				null!=leftExpr.getSymbol() && null!=leftExpr.getSymbol().getCGScope() &&
				leftExpr.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells()) {

				expr1.getCGScope().setParent(cgScope);
				expr2.getCGScope().setParent(cgScope);

				rightExpr.codeGen(cg, false, excs);
				leftExpr.codeGen(cg, false, excs);
				cg.cellsOpCells(cgScope,
								((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, operator,
								((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType);
				return result;
			}
			else if(null!=rightExpr.getSymbol() && null!=rightExpr.getSymbol().getCGScope() && rightExpr.getSymbol().getCGScope() instanceof CGCellsScope &&
					null!=((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells()) {

				expr1.getCGScope().setParent(cgScope);
				expr2.getCGScope().setParent(cgScope);
				// Определяем максмальный размер операнда
				int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());
				rightExpr.codeGen(cg, false, excs);
				expr1.getCGScope().setParent(cgScope);
				expr2.getCGScope().setParent(cgScope);

				if(leftExpr instanceof LiteralExpression) {
					CGCells leftCells = new CGCells(CGCells.Type.CONST, size);
					leftCells.setConst(leftType.isFixedPoint() ? ((LiteralExpression)leftExpr).getFixedValue() : ((LiteralExpression)leftExpr).getNumValue());
					cg.cellsOpCells(cgScope, leftCells, leftType, operator, ((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType);
					return result;
				}
				if(CodegenResult.RESULT_IN_ACCUM==leftExpr.codeGen(cg, true, excs)) {
					cg.cellsOpCells(cgScope, new CGCells(CGCells.Type.ACC, size), leftType, operator,
									((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType);
					return result;
				}
			}
			else if(null!=leftExpr.getSymbol() && null!=leftExpr.getSymbol().getCGScope() && leftExpr.getSymbol().getCGScope() instanceof CGCellsScope &&
					null!=((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells()) {

				expr1.getCGScope().setParent(cgScope);
				expr2.getCGScope().setParent(cgScope);

				// Определяем максмальный размер операнда
				int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());
				leftExpr.codeGen(cg, false, excs);
				if(rightExpr instanceof LiteralExpression) {
					CGCells rightCells = new CGCells(CGCells.Type.CONST, size);
					rightCells.setConst(rightType.isFixedPoint() ? ((LiteralExpression)rightExpr).getFixedValue() : ((LiteralExpression)rightExpr).getNumValue());
					cg.cellsOpCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, operator, rightCells, rightType);
					return result;
				}
				if(CodegenResult.RESULT_IN_ACCUM==rightExpr.codeGen(cg, true, excs)) {
					cg.cellsOpCells(cgScope, ((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType, operator,
									new CGCells(CGCells.Type.ACC, size), rightType);
					return result;
				}
			}
		}
		catch(Exception ex) {
			System.out.println("TODO:" + ex.getMessage());
		}*/

		//================ VarFieldExpression ================================
		if(expr2 instanceof VarFieldExpression) {
			if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}
			expr2.codeGen(cg, false, excs);
			cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), expr1.getType(), operator, ((CGCellsScope)expr2.getSymbol().getCGScope()).getCells(),
							expr2.getType(), cast, excs);
		}
		//================ LiteralExpression ================================
		else if(expr2 instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr2;
			//================ LiteralExpression ==== OTHER ================================
			if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}
			expr2.codeGen(cg, false, excs);			
			cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), expr1.getType(), operator,
							new CGCells(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue()), expr2.getType(), cast, excs);
			//cg.constAction(cgScope, new CGCells(CGCells.Type.ACC), operator, isFixed ? leConst.getFixedValue() : leConst.getNumValue(), isFixed, excs); //TODO cast
		}
		//================ ArrayExpression ================================
		else if(expr2 instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)expr2;
			//================ ArrayExpression ==== LITERAL ================================
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}
				expr1.codeGen(cg, false, excs);
				cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), expr2.getType(), operator,
								new CGCells(le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue()), expr1.getType(), cast, excs);
//				cg.constAction(	cgScope, new CGCells(CGCells.Type.ACC), operator, le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue(),
//								le.getType().isFixedPoint(), excs); //TODO CAST
			}
			//================ ArrayExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=leftExpr.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + leftExpr);
				}					

				// Определяем максмальный размер операнда
				int size = (null !=cast ? cast.getSize() : leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
				cg.pushAccBE(leftExpr.getCGScope(), size, false);
				if(CodegenResult.RESULT_IN_ACCUM != rightExpr.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + rightExpr);
				}
				cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), rightExpr.getType(), operator, new CGCells(CGCells.Type.STACK, size),
								leftExpr.getType(), cast, excs);
			}
		}
		else if(!operator.isCommutative() && leftExpr instanceof LiteralExpression) {
			// Сюда попадаем, если слева константа и не коммутативная операция - мы не можем ее просто перенести вправо, и использовать как константу
			// Значит вычисляем проавую часть помещая ее в стек, а затем помещаем константу в аккумулятор
			// Нужно изменить порядок обработки аргументов, что бы первым выполнялся правыйй аргумент
			cgScope.swap(expr1.getCGScope(), expr2.getCGScope());
			
			// Вычисляем правый опреанд в аккумулятор
			if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + rightExpr);
			}
			// Определяем максмальный размер операнда
			int size = (null !=cast ? cast.getSize() : leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			//Помещаем значение правого операнда из аккумулятора в стек
			cg.pushAccBE(rightExpr.getCGScope(), size, false);

			// Помещаем левый операнд(константу) в аккумулятор
			if(CodegenResult.RESULT_IN_ACCUM != leftExpr.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + leftExpr);
			}
			// Выполняем вычиление
			cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), leftExpr.getType(), operator, new CGCells(CGCells.Type.STACK, size),
							rightExpr.getType(), cast, excs);
		}
		//================ Other ================================
		else {
			// В стек должен быть помещен обязательно правый операнд(expr2), а левый будет в аккумуляторе
			
			// Также нужно убедиться, что первым в кодогенерации будет (expr2)
			cgScope.swap(expr1.getCGScope(), expr2.getCGScope());
			
			// Логика для выражения PERIODS[b&0x0f]>>((b>>4)&0x07); верная
			
			
			// Определяем максимальный размер операнда
			int size = (null !=cast ? cast.getSize() : leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr2);
			}
			// Помещаем в стек значение первого операнда (после его отстройки)
			cg.pushAccBE(expr2.getCGScope(), size, false);

			if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}					
			// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), expr1.getType(), operator,
							new CGCells(CGCells.Type.STACK, size), expr2.getType(), cast, excs);
		}
		
		releaseVars(cg);
		return result;
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
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof ArithmeticExpression)) return false;
		ArithmeticExpression expr2 = (ArithmeticExpression)obj;
		if(operator!=expr2.getOperator()) return false;
		if(!Objects.equals(leftExpr, expr2.getLeft())) return false;
		return Objects.equals(rightExpr, expr2.getRight());
	}
}