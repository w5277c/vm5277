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
import ru.vm5277.common.lexer.Operator;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.compiler.Instance;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.Scope;

public class ComparisonExpression extends BinaryExpression {
	
    ComparisonExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode left, Operator operator, ExpressionNode right) {
        super(inst, tb, sp, left, operator, right);
		
		// Результат сравнения - всегда boolean
		type = VarType.BOOL;
    }
    
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());

		result&=rightExpr.postAnalyze(scope, cg, null);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(rightExpr);
			if(null!=resolved) {
				rightExpr = resolved;
			}
		}

		// Анализ операндов
		result&=leftExpr.postAnalyze(scope, cg, null);
		if(result) {
			// Резолвинг QualifiedPathExpression
			ExpressionNode resolved = resolveQualifiedPathExpr(leftExpr);
			if(null!=resolved) {
				leftExpr = resolved;
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
		
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(disabled) return null;
		excs.setSourcePosition(sp);
		
		//TODO операции сравнения не сохраняют результат в аккумулятор, это делают(к примеру) логические операции.
		CodegenResult result = null;

		//CGScope cgs = null == parent ? cgScope : parent;
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Похоже для всех выражений должны быть удовлетворены зависимости
		depCodeGen(cg, expr1.getSymbol(), excs);
		depCodeGen(cg, expr2.getSymbol(), excs);
		
		// Оптимизация порядка для коммутативных операций
		//TODO не работает, так как позиция CGScope устанавливается в postAnalyze для каждого выражения
		if(operator.isCommutative()) {
			if(leftExpr instanceof LiteralExpression || (!(rightExpr instanceof LiteralExpression) && leftExpr instanceof VarFieldExpression)) {
				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}

		CGBranch branch = null;
		CGScope _scope = cgScope;
		while(null!=_scope) {
			branch = _scope.getBranch();
			if(null!=branch) break;
			_scope = _scope.getParent();
		}

		if(	null!=rightExpr.getSymbol() && null!=rightExpr.getSymbol().getCGScope() &&
			rightExpr.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells() &&
			null!=leftExpr.getSymbol() && null!=leftExpr.getSymbol().getCGScope() &&
			leftExpr.getSymbol().getCGScope() instanceof CGCellsScope && null!=((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells()) {

			expr1.getCGScope().setParent(cgScope);
			expr2.getCGScope().setParent(cgScope);

			rightExpr.codeGen(cg, false, excs);
			leftExpr.codeGen(cg, false, excs);
			cg.cellsCpCells(cgScope,
							((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType,
							((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType,
							operator, isInvert, opOr, (CGBranch)branch);
			return result;
		}
		else if(null!=rightExpr.getSymbol() && null!=rightExpr.getSymbol().getCGScope() && rightExpr.getSymbol().getCGScope() instanceof CGCellsScope &&
				null!=((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells()) {

			expr1.getCGScope().setParent(cgScope);
			expr2.getCGScope().setParent(cgScope);
			// Определяем максмальный размер операнда
			int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());
			rightExpr.codeGen(cg, false, excs);

			if(leftExpr instanceof LiteralExpression) {
				CGCells leftCells = new CGCells(CGCells.Type.CONST, size);
				leftCells.setConst(leftType.isFixedPoint() ? ((LiteralExpression)leftExpr).getFixedValue() : ((LiteralExpression)leftExpr).getNumValue());
				cg.cellsCpCells(cgScope,
								leftCells, leftType,
								((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType,
								operator, isInvert, opOr, (CGBranch)branch);
				return result;
			}
			if(CodegenResult.RESULT_IN_ACCUM==leftExpr.codeGen(cg, true, excs)) {
				cg.cellsCpCells(cgScope,
								new CGCells(CGCells.Type.ACC, size), leftType,
								((CGCellsScope)rightExpr.getSymbol().getCGScope()).getCells(), rightType,
								operator, isInvert, opOr, (CGBranch)branch);
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
				cg.cellsCpCells(cgScope,
								((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType,
								rightCells, rightType,
								operator, isInvert, opOr, (CGBranch)branch);
				return result;
			}
			if(CodegenResult.RESULT_IN_ACCUM==rightExpr.codeGen(cg, true, excs)) {
				cg.cellsCpCells(cgScope,
								((CGCellsScope)leftExpr.getSymbol().getCGScope()).getCells(), leftType,
								new CGCells(CGCells.Type.ACC, size), rightType,
								operator, isInvert, opOr, (CGBranch)branch);
				return result;
			}
		}
		else if(leftExpr instanceof LiteralExpression) {
			expr1.getCGScope().setParent(cgScope);
			expr2.getCGScope().setParent(cgScope);
			// Определяем максмальный размер операнда
			int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());

			CGCells leftCells = new CGCells(CGCells.Type.CONST, size);
			leftCells.setConst(leftType.isFixedPoint() ? ((LiteralExpression)leftExpr).getFixedValue() : ((LiteralExpression)leftExpr).getNumValue());
			if(CodegenResult.RESULT_IN_ACCUM==rightExpr.codeGen(cg, true, excs)) {
				cg.cellsCpCells(cgScope, leftCells, null, new CGCells(CGCells.Type.ACC, size), rightType, operator, isInvert, opOr, (CGBranch)branch);
				return result;
			}
		}
		else if(rightExpr instanceof LiteralExpression) {
			expr1.getCGScope().setParent(cgScope);
			expr2.getCGScope().setParent(cgScope);
			// Определяем максмальный размер операнда
			int size = (leftType.getSize()>rightType.getSize() ? leftType.getSize() : rightType.getSize());

			CGCells rightCells = new CGCells(CGCells.Type.CONST, size);
			rightCells.setConst(rightType.isFixedPoint() ? ((LiteralExpression)rightExpr).getFixedValue() : ((LiteralExpression)rightExpr).getNumValue());
			if(CodegenResult.RESULT_IN_ACCUM==leftExpr.codeGen(cg, true, excs)) {
				cg.cellsCpCells(cgScope, new CGCells(CGCells.Type.ACC, size), leftType, rightCells, null, operator, isInvert, opOr, (CGBranch)branch);
				return result;
			}
		}
		throw new CompileException("Unsupported expression:" + getFullInfo());
	}
}