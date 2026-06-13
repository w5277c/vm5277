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
import ru.vm5277.common.enums.PlatformType;
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
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Instance;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.Scope;

public class ArithmeticExpression extends BinaryExpression {
	
    ArithmeticExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right) {
        super(inst, tb, sp, left, op, right);
    }
    
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());

		// Анализ операндов
		result&=leftExpr.postAnalyze(scope, cg, null);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(leftExpr);
			if(null!=resolved) {
				leftExpr = resolved;
			}
		}

		result&=rightExpr.postAnalyze(scope, cg, null);
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
						isZero = ((Double)val==0.0);
					}
					else if(val instanceof Number) {
						isZero = (((Number)val).longValue()==0L);
					}

					if(isZero) {
						markError("Division by zero");
						result = false;
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
				// Для арифметических операций возвращаем более широкий тип
				type = leftType.getSize()>=rightType.getSize() ? leftType : rightType;
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
	//TODO главная проблема - порядок cgScope(в том числе и выражений) задается в postAnalyze. Мы не пожем просто поп порядку записывать в cgScope, нужно
	//учитывать порядок leftExpr.cgScope и rightExpr.cgScope (cм. //================ ArrayExpression ==== OTHER ================================
	@Override
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(disabled) return null;
		excs.setSourcePosition(sp);
		
		Object result = CodegenResult.RESULT_IN_ACCUM;
		
		//CGScope cgs = null == parent ? cgScope : parent;
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Похоже для всех выражений должны быть удовлетворены зависимости
		depCodeGen(cg, expr1.getSymbol(), excs);
		depCodeGen(cg, expr2.getSymbol(), excs);
		
		// Оптимизация порядка для коммутативных операций
		if(operator.isCommutative()) {
			int leftTypeSize = (-1==leftExpr.getType().getSize() ? cg.getRefSize() : leftExpr.getType().getSize());
			int rightTypeSize = (-1==rightExpr.getType().getSize() ? cg.getRefSize() : rightExpr.getType().getSize());

			expr1 = (leftTypeSize>=rightTypeSize ? leftExpr : rightExpr);
			expr2 = (leftTypeSize>=rightTypeSize ? rightExpr : leftExpr);

			//AVR не умеет выполнять сложение с константой компактно
			//TODO Тщательно проверить
			if(	(leftExpr instanceof LiteralExpression && PlatformType.AVR!=cg.getDevice().getPlatform().getType()) ||
				(!(rightExpr instanceof LiteralExpression) && leftExpr instanceof VarFieldExpression)) {

				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}

		expr1.getCGScope().setParent(cgScope);
		expr2.getCGScope().setParent(cgScope);
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
//			CGCellsScope cScope2 = (CGCellsScope)expr2.getSymbol().getCGScope();
			//================ VarFieldExpression ==== LITERAL ================================
/*			if(expr1 instanceof LiteralExpression) {
			// Чушь собачья, я не могу просто взять и загрузить первым операндом expr2 (ломает деление и вычитание!)
			LiteralExpression le = (LiteralExpression)expr1;
			if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, cgs, true, excs)) {
			throw new CompileException("Accum not used for operand:" + expr1);
			}
			//TODO!!! похоже ошибочная логика во многих местах - нужны тесты fixed с не fixed значениями
			boolean isFixed = expr1.getType().isFixedPoint() || expr2.getType().isFixedPoint();
			if(isFixed && !expr2.getType().isFixedPoint()) {
			cgs.append(cg.accCast(expr2.getType(), expr1.getType()));
			}
			cg.constAction(cgs, operator, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed(), excs);
			}*/
			//================ VarFieldExpression ==== ARRAY ================================
/*TODO вроде как попаадает под универсальный случай, надо проверить
			else if(expr1 instanceof ArrayExpression) {
			expr1.codeGen(cg, cgs, true);
			if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
			cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
			}
			cg.cellsAction(cgs, cScope2.getCells(), operator, VarType.FIXED == expr2.getType());
			}
			 */
			//================ VarFieldExpression ==== OTHER ================================
//			else {
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				boolean isFixed = expr1.getType().isFixedPoint() || expr2.getType().isFixedPoint();
				if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumLock(VarType.FIXED);
				cg.cellsAction(cgScope, new CGCells(CGCells.Type.ACC), operator, ((CGCellsScope)expr2.getSymbol().getCGScope()).getCells(),
								expr2.getType().isFixedPoint(), expr2.getType().isFixedPoint(), excs);
				if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumUnlock();
//			}
		}
		//================ LiteralExpression ================================
		else if(expr2 instanceof LiteralExpression) {
			LiteralExpression leConst = (LiteralExpression)expr2;
			//================ LiteralExpression ==== OTHER ================================
			if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}
			// Проверка на необходимость преобразования аккумулятора в тип fixed
			boolean isFixed = expr1.getType().isFixedPoint() || expr2.getType().isFixedPoint();
			if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumLock(VarType.FIXED);
			cg.constAction(cgScope, new CGCells(CGCells.Type.ACC), operator, isFixed ? leConst.getFixedValue() : leConst.getNumValue(), isFixed, excs);
			if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumUnlock();
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
				if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumLock(VarType.FIXED);
				cg.constAction(	cgScope, new CGCells(CGCells.Type.ACC), operator, le.getType().isFixedPoint() ? le.getFixedValue() : le.getNumValue(),
								le.getType().isFixedPoint(), excs);
				if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumUnlock();
			}
			//================ ArrayExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=leftExpr.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + leftExpr);
				}					

				// Определяем максмальный размер операнда
				int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());
				cg.pushAccBE(leftExpr.getCGScope(), size);
				if(CodegenResult.RESULT_IN_ACCUM != rightExpr.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + rightExpr);
				}
				if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumLock(VarType.FIXED);
				cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), operator, new CGCells(CGCells.Type.STACK, size),
								expr1.getType().isFixedPoint(), expr2.getType().isFixedPoint(), excs);
				if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumUnlock();
			}
		}
		//================ Other ================================
		else {
			if(CodegenResult.RESULT_IN_ACCUM!=leftExpr.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + leftExpr);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			cg.pushAccBE(leftExpr.getCGScope(), size);
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != rightExpr.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + rightExpr);
			}

			// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
			if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumLock(VarType.FIXED);
			cg.cellsAction(	cgScope, new CGCells(CGCells.Type.ACC), operator, new CGCells(CGCells.Type.STACK, size),
							leftType.isFixedPoint(), rightType.isFixedPoint(), excs);
			if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumUnlock();
		}
		
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
}