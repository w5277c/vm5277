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

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.Scope;

public class ComparisonExpression extends BinaryExpression {
	
    ComparisonExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode left, Operator operator, ExpressionNode right) {
        super(tb, mc, sp, left, operator, right);
		
		// Результат сравнения - всегда boolean
		type = VarType.BOOL;
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
			// Запрещаем сравнение reference типов с primitive типами (для любых операторов)
			if((leftType.isReferenceType() && rightType.isPrimitive()) || (leftType.isPrimitive() && rightType.isReferenceType())) {
				markError("Cannot compare reference type " + leftType + " with primitive type " + rightType);
				result = false;
			}
		}
		if(result) {
			// Запрещаем сравнение разных reference типов (кроме NULL)
			if(leftType.isReferenceType() && rightType.isReferenceType() && VarType.NULL!=leftType && VarType.NULL!=rightType && leftType!=rightType) {
				markError("Cannot compare different reference types " + leftType + " and " + rightType);
				result = false;
			}
		}
		if(result) {
			// Запрещаем сравнение NULL с примитивными типами
			if((VarType.NULL==leftType && rightType.isPrimitive()) || (VarType.NULL==rightType && leftType.isPrimitive())) {
				markError("Cannot compare null with primitive type " + (leftType == VarType.NULL ? rightType : leftType));
				result = false;
			}
		}
		if(Operator.EQ!=operator && Operator.NEQ!=operator) {
			if(result) {
				// Запрещаем операции сравнения (кроме равенства/неравенства) для boolean
				if(leftType.isBoolean() || rightType.isBoolean()) {
					markError("Comparison operations (other than == and !=) cannot be applied to bool operands");
					result = false;
				}
			}
			if(result) {
				// Запрещаем операции сравнения (кроме равенства/неравенства) для cstr
				if(VarType.CSTR==leftType || VarType.CSTR==rightType) {
					markError("Comparison operations (other than == and !=) cannot be applied to cstr operands");
					result = false;
				}
			}
			if(result) {			
				// Запрещаем операции сравнения (кроме равенства/неравенства) для null
				if(VarType.NULL==leftType || VarType.NULL==rightType) {
					markError("Comparison operations (other than == and !=) cannot be applied to null");
					result = false;
				}
			}
			if(result) {
				// Запрещаем операции сравнения (кроме равенства/неравенства) для references
				if(leftType.isReferenceType() || rightType.isReferenceType()) {
					markError("Comparison operations (other than == and !=) cannot be applied to reference types");
					result = false;
				}
			}
		}
		
		cg.leaveExpression();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum) throws CompileException {
		if(disabled) return null;
		
		//TODO операции сравнения не сохраняют результат в аккумулятор, это делают(к примеру) логические операции.
		CodegenResult result = null;

		CGScope cgs = null == parent ? cgScope : parent;
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Похоже для всех выражений должны быть удовлетворены зависимости
		depCodeGen(cg, expr1.getSymbol());
		depCodeGen(cg, expr2.getSymbol());
		
		// Оптимизация порядка для коммутативных операций
		if(operator.isCommutative()) {
			if(leftExpr instanceof LiteralExpression || (!(rightExpr instanceof VarFieldExpression) && leftExpr instanceof VarFieldExpression)) {
				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}

		CGBranch branch = null;
		CGScope _scope = cgs;
		while(null!=_scope) {
			branch = _scope.getBranch();
			if(null!=branch) break;
			_scope = _scope.getParent();
		}

		//================ VarFieldExpression ================================
		if(expr2 instanceof VarFieldExpression) {
			CGCellsScope cScope2 = (CGCellsScope)expr2.getSymbol().getCGScope();
			//================ VarFieldExpression ==== LITERAL ================================
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				expr2.codeGen(cg, cgs, false);
				cg.constCond(cgs, cScope2.getCells(), operator, le.getNumValue(), isInvert, opOr, (CGBranch)branch);
			}
			//================ VarFieldExpression ==== ARRAY ================================
/*TODO вроде как попаадает под универсальный случай, надо проверить
			else if(expr1 instanceof ArrayExpression) {
				expr1.codeGen(cg, cgs, true);
				if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
				}
				cg.cellsCond(cgs, cScope2.getCells(), operator, isInvert, opOr, (CGBranch)branch);
			}
*/
			//================ VarFieldExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
				}
				cg.cellsCond(cgs, cScope2.getCells(), operator, isInvert, opOr, (CGBranch)branch);
			}
		}
		//================ LiteralExpression ================================
		else if(expr2 instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)expr2;
			//================ LiteralExpression ==== VAR/FIELD ================================
			if(expr1 instanceof VarFieldExpression) {
				expr1.codeGen(cg, cgs, false);
				CGCellsScope cScope = (CGCellsScope)expr1.getSymbol().getCGScope();
				cg.constCond(cgs, cScope.getCells(), operator, le.getNumValue(), isInvert, opOr, branch);
			}
			else if(expr1 instanceof ArrayExpression) {
				expr1.codeGen(cg, cgs, false);
				CGCellsScope cScope = (CGCellsScope)expr1.getSymbol().getCGScope();
				cg.constCond(cgs, cScope.getCells(), operator, le.getNumValue(), isInvert, opOr, branch);
			}
			//================ LiteralExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				cg.constCond(cgs, new CGCells(CGCells.Type.ACC), operator, le.getNumValue(), isInvert, opOr, branch);
			}
		}
		//================ ArrayExpression ================================
		else if(expr2 instanceof ArrayExpression) {
			ArrayExpression ae = (ArrayExpression)expr2;
			//================ ArrayExpression ==== LITERAL ================================
			if(expr1 instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)expr1;
				expr2.codeGen(cg, cgs, false);
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				cg.constCond(cgs, cScope.getCells(), operator, le.getNumValue(), isInvert, opOr, (CGBranch)branch);
			}
			//================ ArrayExpression ==== VAR/FIELD ================================
			else if(expr1 instanceof VarFieldExpression) {
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				expr2.codeGen(cg, cgs, false);
				CGCellsScope cScope = (CGCellsScope)expr2.getSymbol().getCGScope();
				cg.cellsCond(cgs, cScope.getCells(), operator, isInvert, opOr, (CGBranch)branch);
			}
			//================ ArrayExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}					

				// Определяем максмальный размер операнда
				int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());
				cg.pushAccBE(cgs, size);
				if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
					cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
				}
				cg.cellsCond(cgs, new CGCells(CGCells.Type.STACK, size), operator, isInvert, opOr, (CGBranch)branch);
			}
		}
		//================ Other ================================
		else {
			if(CodegenResult.RESULT_IN_ACCUM!=expr2.codeGen(cg, cgs, true)) {
				throw new CompileException("Accum not used for operand:" + expr2);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			cg.pushAccBE(cgs, size);
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}

			// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
			if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
				cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
			}
			cg.cellsCond(cgs, new CGCells(CGCells.Type.STACK, size), operator, isInvert, opOr, (CGBranch)branch);
		}
		
		return result;
	}
}