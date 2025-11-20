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
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.Operator;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CGArrCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Main;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.ArrayExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.InitNodeHolder;
import ru.vm5277.compiler.semantic.Scope;

public class AssignExpression extends BinaryExpression {
	
    AssignExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right) {
        super(tb, mc, sp, left, op, right);
    }
    
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(null!=postResult) return postResult;
		postResult = true;
		
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(toString());

		// Анализ операндов
		postResult&=leftExpr.postAnalyze(scope, cg);
		if(postResult) {
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

		postResult&=rightExpr.postAnalyze(scope, cg);
		if(postResult) {
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

		if(postResult) {
			// Проверка final переменных
			if(leftExpr.getSymbol().isFinal()) {
				markError("Cannot assign to final var/field: " + leftExpr);
				postResult = false;
			}
		}
		
		if(postResult) {
			if(leftExpr instanceof VarFieldExpression) {
				if(rightExpr instanceof UnaryExpression) {
					Operator unaryOp = ((UnaryExpression)rightExpr).getOperator();
					if(Operator.MINUS==unaryOp && !rightExpr.getType().isFixedPoint()) {
						if(AssemblerInterface.STRICT_STRONG==Main.getStrictLevel()) {
							markError("Unary " + ((UnaryExpression)rightExpr).getOperator() + " requires fixed type");
							postResult = false;
						}
						else if(AssemblerInterface.STRICT_LIGHT==Main.getStrictLevel()) {
							markWarning("Unary " + ((UnaryExpression)rightExpr).getOperator() + " requires fixed type");
						}
					}
					else if(Operator.PRE_INC==unaryOp || Operator.PRE_DEC==unaryOp || Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
						UnaryExpression ue = (UnaryExpression)rightExpr;
						if(	ue.getOperand() instanceof VarFieldExpression &&
							((VarFieldExpression)leftExpr).getName().equals(((VarFieldExpression)ue.getOperand()).getName())) {
							
							if(AssemblerInterface.STRICT_STRONG==Main.getStrictLevel()) {
								if(Operator.POST_INC==unaryOp || Operator.POST_DEC==unaryOp) {
									markError("Redudnand statement has no practical effect");
								}
								else {
									markError("Redudnand statement");
								}
								postResult = false;
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
			else if(leftExpr instanceof ArrayExpression) {
				//TODO проверить
				if(((ArrayExpression)leftExpr).getDepth()!=((ArrayExpression)leftExpr).getPathExpr().getType().getArrayDepth()) {
					markError("Array assignment dimension mismatch");
					postResult = false;
				}
			}
			else {
				markError("Invalid left operand for assignment: " + leftExpr);
				postResult = false;
			}
		}
		
		if(postResult) {
			if(leftExpr instanceof VarFieldExpression && rightExpr instanceof VarFieldExpression) {
				if(((VarFieldExpression)leftExpr).isSameVarFiled((VarFieldExpression)rightExpr)) {
					markWarning("Self-assignment for '" + ((VarFieldExpression)rightExpr).getName() + "' has no effect");
					disable();
				}
			}
		}

		if(postResult) {
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
				postResult = false;
			}

			if(postResult) {
				if(!isCompatibleWith(scope, leftType, rightType)) {
					if(VarType.FIXED==type && rightExpr instanceof LiteralExpression && rightType.isIntegral()) {
						long num = ((LiteralExpression)rightExpr).getNumValue();
						if(num<VarType.FIXED_MIN || num>VarType.FIXED_MAX) {
							markError("Type mismatch: cannot assign " + rightType + " to " + type);
							postResult = false;
						}
					}
					else {
						markError("Type mismatch: cannot assign " + rightType + " to " + type);
						postResult = false;
					}
				}
			}
		}

		cg.leaveExpression();
		debugAST(this, POST, false, postResult, getFullInfo());
		return postResult;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum) throws CompileException {
		if(disabled) return null;
		
		CGScope cgs = null == parent ? cgScope : parent;
		
		// Для всех выражений должны быть удовлетворены зависимости
		depCodeGen(cg, leftExpr.getSymbol());
		depCodeGen(cg, rightExpr.getSymbol());

		// В присваивании размер аккумулятора всегда соотвествует левому выражению
		cg.accResize(leftType);

		//================ VarFieldExpression ================================
		if(leftExpr instanceof VarFieldExpression) {
			VarFieldExpression leftVfe = (VarFieldExpression)leftExpr;
			leftExpr.codeGen(cg, cgs, false);
			CGCellsScope leftCScope = (CGCellsScope)leftVfe.getSymbol().getCGScope();

			//================ VarFieldExpression ==== NULL ================================
			if(VarType.NULL==rightType) { // В левой части reference type (гарантирует postAnalyze)
				// Сохраням HEAP индексный регистр
				cg.pushHeapReg(cgs);
				// Декремент счетчика ссылок
				cg.updateClassRefCount(cgs, leftCScope.getCells(), false);
				// Восстанавливаем HEAP индексный регистр
				cg.popHeapReg(cgs);
				// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
				cgs.append(cg.constToCells(cgs, 0x00, leftCScope.getCells(), false));
			}
			//================ VarFieldExpression ==== VAR/FIELD ================================
			else if(rightExpr instanceof VarFieldExpression) {
				VarFieldExpression rightVfe = (VarFieldExpression)rightExpr;
				CGCellsScope rightCScope = (CGCellsScope)rightExpr.getSymbol().getCGScope();				
				if(CodegenResult.RESULT_IN_ACCUM!=rightVfe.codeGen(cg, cgs, true)) {
					cg.cellsToAcc(cgs, rightCScope);
				}
			    cg.accToCells(cgs, leftCScope);
			}
			//================ VarFieldExpression ==== ARRAY ================================
			else if(rightExpr instanceof ArrayExpression) {
				ArrayExpression ae = (ArrayExpression)rightExpr;
				if(CodegenResult.RESULT_IN_ACCUM!=ae.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + rightExpr);
				}
				cgs.append(cg.accCast(rightType, leftType));
				cg.accToCells(cgs, leftCScope);
			}
			//================ VarFieldExpression ==== LITERAL ================================
			else if(rightExpr instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)rightExpr;
				// Определяем использование FIXED
				boolean isFixed = le.isFixed() || VarType.FIXED == leftType;
				cgs.append(cg.constToCells(cgs, isFixed ? le.getFixedValue() : le.getNumValue(), leftCScope.getCells(), isFixed));
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
					rightExpr.codeGen(cg, cgs, false);
				}
				else {
					if(CodegenResult.RESULT_IN_ACCUM==rightExpr.codeGen(cg, cgs, true)) {
						cgs.append(cg.accCast(rightType, leftType));
						cg.accToCells(cgs, leftCScope);
					}
				}
			}
			//================ VarFieldExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + rightExpr);
				}
				else {
					cgs.append(cg.accCast(rightType, leftType));
					cg.accToCells(cgs, leftCScope);
				}
			}
		}
		//================ ArrayExpression ================================
		else {
			ArrayExpression leftAe = (ArrayExpression)leftExpr;
			CGCellsScope leftCScope = (CGCellsScope)leftAe.getSymbol().getCGScope();

			//================ ArrayExpression ==== NULL ================================
			if(VarType.NULL==rightType) { // В левой части reference type (гарантирует postAnalyze)
				leftExpr.codeGen(cg, cgs, false);
				//TODO нужно проверить!
				// Проверяем на View
				boolean isView = false;
				AstHolder ah = (AstHolder)leftExpr.getSymbol();
				if(ah.getNode() instanceof InitNodeHolder) {
					InitNodeHolder inh = (InitNodeHolder)ah.getNode();
					if(inh.getInitNode() instanceof ArrayExpression) {
						isView = true;
					}
				}
				// Декремент счетчика ссылок
				cg.updateArrRefCount(cgs, leftCScope.getCells(), false, isView);
				// Записываем в ref адрес 0 - при обращении необходимо выдать что-то типа 'null pointer exception'
				cgs.append(cg.constToCells(cgs, 0x00, leftCScope.getCells(), false));
			}
			//================ ArrayExpression ==== ARRAY ================================
			else if(rightExpr instanceof ArrayExpression) {
				if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + rightExpr);
				}
				leftExpr.codeGen(cg, cgs, false);
				cgs.append(cg.accCast(rightType, leftType));
				cg.accToArr(cgs, (CGArrCells)leftCScope.getCells());
			}
			//================ ArrayExpression ==== LITERAL ================================
			else if(rightExpr instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)rightExpr;
				leftExpr.codeGen(cg, cgs, false);
				// Определяем использование FIXED
				boolean isFixed = le.isFixed() || VarType.FIXED == leftType;
				cgs.append(cg.constToCells(cgs, isFixed ? le.getFixedValue() : le.getNumValue(), leftCScope.getCells(), isFixed));
			}
			//================ ArrayExpression ==== OTHER ================================
			else {
				if(CodegenResult.RESULT_IN_ACCUM!=rightExpr.codeGen(cg, cgs, true)) {
					throw new CompileException("Accum not used for operand:" + rightExpr);
				}
				int accumSize = cg.getAccumSize();
				cg.pushAccBE(cgs, accumSize);
				leftExpr.codeGen(cg, cgs, false);
				cg.popAccBE(cgs, accumSize);
				cg.accCast(rightType, leftType);
				cg.accToCells(cgs, (CGCellsScope)leftExpr.getSymbol().getCGScope());
			}
		}

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