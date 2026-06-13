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
import static ru.vm5277.common.enums.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.enums.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.CGBranch;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.enums.CodegenResult;
import ru.vm5277.common.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Instance;
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
	
    protected BinaryExpression(Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode left, Operator op, ExpressionNode right) {
        super(inst, tb, sp);
        
		this.leftExpr = left;
        this.operator = op;
        this.rightExpr = right;
    }
	
	public static BinaryExpression create( Instance inst, TokenBuffer tb, SourcePosition sp, ExpressionNode left, Operator operator, ExpressionNode right) {
		if(operator.isAssignment()) {
			if(Operator.ROR==operator || Operator.ROL==operator) {
				return new AssignExpression(inst, tb, sp, left, operator, right);
			}
			else {
				Operator arithmOp = operator.toArithmetic();
				if(null==arithmOp) {
					return new AssignExpression(inst, tb, sp, left, Operator.ASSIGN, right);
				}
				return new AssignExpression(inst, tb, sp, left, Operator.ASSIGN, BinaryExpression.create(inst, tb, sp, left, arithmOp, right));
			}
		}
		else if(operator.isComparison()) {
			return new ComparisonExpression(inst, tb, sp, left, operator, right);
		}
		else if(operator.isArithmetic() || operator.isBitwise()) {
			return new ArithmeticExpression(inst, tb, sp, left, operator, right);
		}
		return new BinaryExpression(inst, tb, sp, left, operator, right);
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
	public boolean postAnalyze(Scope scope, CodeGenerator cg, CGScope parent) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo() + " type:" + type);
		cgScope = cg.enterExpression(parent, cgScope, toString());
		
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
			if(operator.isBitwise() && (!leftType.isInteger() || !rightType.isInteger())) {
				markError("Bitwise operators require integer operands");
				result = false;
			}
		}

		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}
	
	@Override
	public void codeOptimization(Scope scope, CodeGenerator cg) {
		leftExpr.codeOptimization(scope, cg);
		try {
			ExpressionNode optimizedExpr = leftExpr.optimizeWithScope(scope, cg);
			if(null != optimizedExpr) {
				leftExpr = optimizedExpr;
			}
		}
		catch(CompileException ex) {
			markError(ex);
		}

		rightExpr.codeOptimization(scope, cg);
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
	
	@Override
	public Object codeGen(CodeGenerator cg, boolean toAccum, CGExcs excs) throws CompileException {
		return codeGen(cg, false, false, toAccum, excs);
	}
	public Object codeGen(CodeGenerator cg, boolean isInvert, boolean opOr, boolean toAccum, CGExcs excs) throws CompileException {
		if(disabled) return null;
		excs.setSourcePosition(sp);
		
		CodegenResult result = CodegenResult.RESULT_IN_ACCUM;

//		CGScope cgs = null == parent ? cgScope : parent;
		
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
			if(leftExpr instanceof LiteralExpression || (!(rightExpr instanceof LiteralExpression) && leftExpr instanceof VarFieldExpression)) {
				expr1 = rightExpr;
				expr2 = leftExpr;
			}
		}
		
		CGBranch branch = null;
		if(operator.isLogical()) {
			CGScope _scope = cgScope;
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
				if(CodegenResult.RESULT_IN_ACCUM!=expr1.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr1);
				}
				// Сохраняем результат левого операнда
				cg.pushAccBE(cgScope, 1); // boolean занимает 1 байт

				// Генерируем код для правого операнда
				if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, true, excs)) {
					throw new CompileException("Accum not used for operand:" + expr2);
				}
				// Выполняем логическую операцию со значением в стеке
				cg.cellsAction(cgScope, new CGCells(CGCells.Type.ACC), operator, new CGCells(CGCells.Type.STACK, 1), false, false, excs);
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
					if(CodegenResult.RESULT_IN_ACCUM != ((BinaryExpression)expr1).codeGen(cg, isInvert, Operator.OR == operator, false, excs)) {
						result = null;
					}
				}
				else if(expr1 instanceof VarFieldExpression) {
					((VarFieldExpression)expr1).codeGen(cg, isInvert, Operator.OR == operator, branch, excs);
					((VarFieldExpression)expr1).postCodeGen(cg);
					result = null;
				}
				else if(expr1 instanceof UnaryExpression) {
					// Работа с branch - похоже артефак который в добавок ломал логику
//					((CGBranch)branch).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_NOT_END, true));
					((UnaryExpression)expr1).codeGen(cg, isInvert, Operator.OR == operator, false, excs);
//					CGLabelScope lbScope = ((CGBranch)branch).popEnd();
					//TODO проверить(не стандартный CGScope)
					//cg.jump(expr1.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr1.getCGScope().append(lbScope);
//					cg.jump(cgs, ((CGBranch)branch).getEnd());
//					cgs.append(lbScope);
				}
				else {
					expr1.codeGen(cg, false, excs);
				}

				if(expr2 instanceof BinaryExpression) {
					if(CodegenResult.RESULT_IN_ACCUM != ((BinaryExpression)expr2).codeGen(cg, isInvert, Operator.OR == operator, false, excs)) {
						result = null;
					}
				}
				else if(expr2 instanceof VarFieldExpression) {
					((VarFieldExpression)expr2).codeGen(cg, isInvert, Operator.OR == operator, (CGBranch)branch, excs);
					((VarFieldExpression)expr2).postCodeGen(cg);
					result = null;
				}
				else if(expr2 instanceof UnaryExpression) {
					// Работа с branch - похоже артефак который в добавок ломал логику
//					((CGBranch)branch).pushEnd(new CGLabelScope(null, null, LabelNames.LOCGIC_NOT_END, true));
					((UnaryExpression)expr2).codeGen(cg, isInvert, Operator.OR == operator, false, excs);
//					CGLabelScope lbScope = ((CGBranch)branch).popEnd();
					//TODO проверить(не стандартный CGScope)
					//cg.jump(expr2.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr2.getCGScope().append(lbScope);
//					cg.jump(cgs, ((CGBranch)branch).getEnd());
//					cgs.append(lbScope);

				}
				else {
					expr2.codeGen(cg, false, excs);
				}

				if(opOr ^ (Operator.OR == operator)) {
					CGLabelScope lbScope = ((CGBranch)branch).popEnd();
					//cg.jump(expr2.getCGScope(), ((CGBranchScope)brScope).getEnd());
					//expr2.getCGScope().append(lbScope);
					cg.jump(cgScope, ((CGBranch)branch).getEnd());
					cgScope.append(lbScope);
				}
			}
		}
		else {
			//CastExpression, BinaryExpression и другие
			if(CodegenResult.RESULT_IN_ACCUM != expr2.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr2);
			}					

			// Определяем максмальный размер операнда
			int size = (leftType.getSize() > rightType.getSize() ? leftType.getSize() : rightType.getSize());
			// Выполняем операцию, левый операнд - аккумулятор, правый операнд - значение на вершине стека
			// Код отстроен и результат может быть только в аккумуляторе, сохраняем его
			cg.pushAccBE(cgScope, size);
			// Строим код для expr1(результат в аккумуляторе)
			if(CodegenResult.RESULT_IN_ACCUM != expr1.codeGen(cg, true, excs)) {
				throw new CompileException("Accum not used for operand:" + expr1);
			}

			if(!leftType.isFixedPoint() && rightType.isFixedPoint()) cg.accumLock(VarType.FIXED);
			cg.cellsAction(cgScope, new CGCells(CGCells.Type.ACC), operator, new CGCells(CGCells.Type.STACK, size),
							expr1.getType().isFixedPoint(), expr2.getType().isFixedPoint(), excs);
			if(operator.isAssignment()) {
				cg.accToCells(cgScope, expr1.getType(), (CGCellsScope)expr1.getSymbol().getCGScope());
			}
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
	
	public String getFullInfo() {
		return getClass().getSimpleName() + "  " + toString();
	}
}