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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.Scope;

public class ArithmeticExpression extends BinaryExpression {
	
    ArithmeticExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right) {
        super(tb, mc, sp, left, op, right);
    }
    
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());

		// Анализ операндов
		result&=leftExpr.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = leftExpr.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					leftExpr = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
			}
		}

		result&=rightExpr.postAnalyze(scope, cg);
		if(result) {
			try {
				ExpressionNode optimizedExpr = rightExpr.optimizeWithScope(scope, cg);
				if(null != optimizedExpr) {
					rightExpr = optimizedExpr;
				}
			}
			catch(CompileException ex) {
				markError(ex);
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
			// Запрещаем операции с bool типами
			if(leftType.isBoolean() || rightType.isBoolean()) {
				markError("Arithmetic operations cannot be applied to bool operands");
				result = false;
			}
		}
		if(result) {
			// Запрещаем операции с CSTR
			if(VarType.CSTR==leftType || VarType.CSTR==rightType) {
				markError("Arithmetic operations cannot be applied to cstr operands");
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
			if(leftType==VarType.FIXED || rightType==VarType.FIXED) {
				type = VarType.FIXED;
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

		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		CGScope oldScope = cg.setScope(cgScope);

		leftExpr.codeOptimization(scope, cg);
		rightExpr.codeOptimization(scope, cg);
		
		try {
			ExpressionNode optimizedExpr = leftExpr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				leftExpr = optimizedExpr;
			}
			optimizedExpr = rightExpr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				rightExpr = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}
		cg.setScope(oldScope);
	}

	//TODO Добавить явные случаи Array + VarField и VarField + Array
	//TODO Проверить порядок операндов в Literal + VarField
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(disabled) return null;
		excs.setSourcePosition(sp);
		
		CGScope cgs = null == parent ? cgScope : parent;
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Похоже для всех выражений должны быть удовлетворены зависимости
		depCodeGen(cg, expr1.getSymbol(), excs);
		depCodeGen(cg, expr2.getSymbol(), excs);
		
		// Оптимизация порядка для коммутативных операций
		if(operator.isCommutative()) {
			if(leftExpr instanceof VarFieldExpression || leftExpr instanceof LiteralExpression) {
				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}

		//================ VarFieldExpression ================================
		if(expr2 instanceof VarFieldExpression) {
			CGCellsScope cScope2 = (CGCellsScope)expr2.getSymbol().getCGScope();
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
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, cgs, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				boolean isFixed = expr1.getType().isFixedPoint() || expr2.getType().isFixedPoint();
				if(isFixed && !expr1.getType().isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
				}
				cg.cellsAction(cgs, cScope2.getCells(), operator, expr2.getType().isFixedPoint(), excs);
//			}
		}
		//================ LiteralExpression ================================
		else if(expr2 instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr2;
			//================ LiteralExpression ==== OTHER ================================
			if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}
			boolean isFixed = expr1.getType().isFixedPoint() || expr2.getType().isFixedPoint();
			if(isFixed && !expr1.getType().isFixedPoint()) {
				cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
			}
			cg.constAction(cgs, operator, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed(), excs);
		}
		//================ ArrayExpression ================================
		else if(expr2 instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)expr2;
			//================ ArrayExpression ==== LITERAL ================================
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, cgs, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}
				//Аккумулятор уже необходимого размера, но нужно проверить на Fixed
				if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
					cgs.append(cg.accCast(expr2.getType(), expr1.getType()));
				}
				//Добавить проверку деления на 0
				cg.constAction(cgs, operator, le.isFixed() ? le.getFixedValue() : le.getNumValue(), le.isFixed(), excs);
			}
			//================ ArrayExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, cgs, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}					

				// Определяем максмальный размер операнда
				int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());
				cg.pushAccBE(cgs, size);
				if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
				}
				cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, size), operator, expr2.getType().isFixedPoint(), excs);
			}
		}
		//================ Other ================================
		else {
			if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr2);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			cg.pushAccBE(cgs, size);
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}

			// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
			if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
				cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
			}
			cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, size), operator, expr2.getType().isFixedPoint(), excs);
		}
		
		return CodegenResult.RESULT_IN_ACCUM;
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