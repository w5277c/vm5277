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
import ru.vm5277.common.Operator;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.scopes.CGMethodScope;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.QualifiedPathExpression;
import ru.vm5277.compiler.nodes.expressions.UnaryExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.Scope;

public class BinaryExpression extends ExpressionNode {
    protected			ExpressionNode	leftExpr;
    protected	final	Operator		operator;
    protected			ExpressionNode	rightExpr;
	protected			VarType			leftType;
	protected			VarType			rightType;
	protected			boolean			isUsed		= false;
	
    protected BinaryExpression(TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right) {
        super(tb, mc, sp);
        
		this.leftExpr = left;
        this.operator = op;
        this.rightExpr = right;
    }
	
	public static BinaryExpression create(	TokenBuffer tb, MessageContainer mc, SourcePosition sp, ExpressionNode left, Operator operator,
											ExpressionNode right) {
		if(operator.isAssignment()) {
			Operator arithmOp = operator.toArithmetic();
			if(null==arithmOp) {
				return new AssignExpression(tb, mc, sp, left, Operator.ASSIGN, right);
			}
			return new AssignExpression(tb, mc, sp, left, Operator.ASSIGN, BinaryExpression.create(tb, mc, sp, left, arithmOp, right));
		}
		else if(operator.isComparison()) {
			return new ComparisonExpression(tb, mc, sp, left, operator, right);
		}
		else if(operator.isArithmetic() || Operator.SHL==operator || Operator.SHR==operator) {
			return new ArithmeticExpression(tb, mc, sp, left, operator, right);
		}
		return new BinaryExpression(tb, mc, sp, left, operator, right);
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
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());
		
		// Проверка наличия операндов
		if(null==leftExpr) {
			markError("Left operand is missing in binary expression");
			result = false;
		}
		if(null==rightExpr) {
			markError("Right operand is missing in binary expression");
			result = false;
		}
		
		// Рекурсивный анализ операндов
		if(result && !leftExpr.preAnalyze()) {
			result = false;
		}
		if(result && !rightExpr.preAnalyze()) {
			result = false;
		}

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());

		if(!leftExpr.declare(scope)) {
			result = false;
		}
		if(!rightExpr.declare(scope)) {
			result = false;
		}
		
		if(result && this instanceof AssignExpression && leftExpr instanceof QualifiedPathExpression) {
			((QualifiedPathExpression)leftExpr).setReassigned();
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo() + (declarationPendingNodes.containsKey(this) ? " [DP]" : ""));
		return result;
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
			if(operator.isLogical()) {
				type = VarType.BOOL;
			}
			else {
				// Возвращаем более широкий тип
				type = leftType.getSize() >= rightType.getSize() ? leftType : rightType;
			}

			// Проверка на недопустимые операции для boolean типов
			if(leftType.isBoolean() || rightType.isBoolean()) {
				// Запрещаем побитовые операции с boolean
				if(operator.isBitwise()) {
					markError("Bitwise operations cannot be applied to boolean operands");
					result = false;
				}
			}
		}

		if(result) {
			// Специфичные проверки операторов
			if(operator.isLogical() &&  (!leftType.isBoolean() || !rightType.isBoolean())) {
				markError("Logical operators require boolean operands");
				result = false;
			}
		}
		if(result) {
			if(operator.isBitwise() && (!leftType.isIntegral() || !rightType.isIntegral())) {
				markError("Bitwise operators require integer operands");
				result = false;
			}
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
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum, CGExcs excs) throws CompileException {
		return codeGen(cg, parent, false, false, toAccum, excs);
	}
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(disabled) return null;
		excs.setSourcePosition(sp);
		
		CodegenResult result = CodegenResult.RESULT_IN_ACCUM;

		CGScope cgs = null == parent ? cgScope : parent;
		
		int leftSize = leftType.getSize();
		int rightSize = rightType.getSize();
		
		// Изначально сохраняем порядок left/right
		ExpressionNode expr1 = leftExpr;
		ExpressionNode expr2 = rightExpr;

		// Похоже для всех выражений должны быть удовлетворены зависимости
		//TODO убрать depCodeGen из кода ниже
		depCodeGen(cg, expr1.getSymbol(), excs);
		depCodeGen(cg, expr2.getSymbol(), excs);
		
		// Оптимизация порядка для коммутативных операций
		if(operator.isCommutative()) {
			if(leftExpr instanceof VarFieldExpression || leftExpr instanceof LiteralExpression) {
				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}
		
		CGBranch branch = null;
		if(operator.isLogical()) {
			CGScope _scope = cgs;
			while(null!=_scope) {
				branch = _scope.getBranch();
				if(null!=branch) break;
				_scope = _scope.getParent();
			}
		}

		if(operator.isLogical()) { // AND или OR
			if(null==branch) {
				// Вычисление результата в аккумуляторе (без условных переходов)
				// Генерируем код для левого операнда
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, cgs, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				// Сохраняем результат левого операнда
				cg.pushAccBE(cgs, 1); // boolean занимает 1 байт

				// Генерируем код для правого операнда
				if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, cgs, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}
				// Выполняем логическую операцию со значением в стеке
				cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, 1), operator, false, excs);
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
					((CGBranch)branch).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_END, true)); //Конец логического блока OR или AND
				}

				if(expr1 instanceof BinaryExpression) {
					if(CodegenResult.RESULT_IN_ACCUM != ((BinaryExpression)expr1).codeGen(cg, cgs, isInvert, Operator.OR == operator, false, excs)) {
						result = null;
					}
				}
				else if(expr1 instanceof VarFieldExpression) {
					((VarFieldExpression)expr1).codeGen(cg, cgs, isInvert, Operator.OR == operator, branch, excs);
					((VarFieldExpression)expr1).postCodeGen(cg, cgs);
					result = null;
				}
				else if(expr1 instanceof UnaryExpression) {
					((CGBranch)branch).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_NOT_END, true));
					((UnaryExpression)expr1).codeGen(cg, null, isInvert, Operator.OR == operator, false, excs);
					CGLabelScope lbScope = ((CGBranch)branch).popEnd();
					//TODO проверить(не стандартный CGScope)
					//cg.jump(expr1.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr1.getCGScope().append(lbScope);
					cg.jump(cgs, ((CGBranch)branch).getEnd());
					cgs.append(lbScope);
				}
				else {
					expr1.codeGen(cg, cgs, false, excs);
				}

				if(expr2 instanceof BinaryExpression) {
					if(CodegenResult.RESULT_IN_ACCUM != ((BinaryExpression)expr2).codeGen(cg, cgs, isInvert, Operator.OR == operator, false, excs)) {
						result = null;
					}
				}
				else if(expr2 instanceof VarFieldExpression) {
					((VarFieldExpression)expr2).codeGen(cg, cgs, isInvert, Operator.OR == operator, (CGBranch)branch, excs);
					((VarFieldExpression)expr2).postCodeGen(cg, cgs);
					result = null;
				}
				else if(expr2 instanceof UnaryExpression) {
					((CGBranch)branch).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_NOT_END, true));
					((UnaryExpression)expr2).codeGen(cg, null, isInvert, Operator.OR == operator, false, excs);
					CGLabelScope lbScope = ((CGBranch)branch).popEnd();
					//TODO проверить(не стандартный CGScope)
					//cg.jump(expr2.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr2.getCGScope().append(lbScope);
					cg.jump(cgs, ((CGBranch)branch).getEnd());
					cgs.append(lbScope);

				}
				else {
					expr2.codeGen(cg, null, false, excs);
				}

				if(opOr ^ (Operator.OR == operator)) {
					CGLabelScope lbScope = ((CGBranch)branch).popEnd();
					//cg.jump(expr2.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr2.getCGScope().append(lbScope);
					cg.jump(cgs, ((CGBranch)branch).getEnd());
					cgs.append(lbScope);
				}
			}
		}
		else {
			//CastExpression, BinaryExpression и другие
			if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr2);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			// Код отстроен и результат может быть только в аккумуляторе, сохраняем его
			cg.pushAccBE(cgs, size);
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, cgs, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}

			// Аккумулятор уже необходимого размера, но нужно проверить на Fixed
			if(expr1.getType().isFixedPoint() ^ expr2.getType().isFixedPoint()) {
				cgs.append(cg.accCast(expr1.getType(), expr2.getType()));
			}
			cg.cellsAction(cgs, new CGCells(CGCells.Type.STACK, size), operator, VarType.FIXED == expr2.getType(), excs);
			if(operator.isAssignment()) {
				cg.accToCells(cgs, (CGCellsScope)expr1.getSymbol().getCGScope());
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
		return leftExpr + ", " + operator + ", " + rightExpr;
	}
	
	public String getFullInfo() {
		return getClass().getSimpleName() + "  " + toString();
	}
}