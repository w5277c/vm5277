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
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.cg.scopes.CGFieldScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.semantic.AstHolder;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.VarSymbol;

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
	public VarType getType(Scope scope) throws SemanticException {
		// Получаем типы операндов
		VarType leftType = leftExpr.getType(scope);
		VarType rightType = rightExpr.getType(scope);

		// Проверяем совместимость типов
		if (!isCompatibleWith(scope, leftType, rightType)) {
			throw new SemanticException("Type mismatch in binary operation: " + leftType + " " + operator + " " + rightType);
		}

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
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			cgScope = cg.enterExpression();
			// Анализ операндов
			if (!leftExpr.postAnalyze(scope, cg) || !rightExpr.postAnalyze(scope, cg)) return false;

			leftType = leftExpr.getType(scope);
			rightType = rightExpr.getType(scope);

			if(operator.isAssignment()) {
				if(leftExpr instanceof VarFieldExpression) {
					VarFieldExpression varExpr = (VarFieldExpression) leftExpr;
					Symbol symbol = scope.resolve(varExpr.getValue());
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
						ClassScope classScope = scope.getThis().resolveClass(targetType.getClassName());
						if (null != classScope) {
							Symbol field = classScope.getFields().get(fieldName);
							if (null != field && field.isFinal()) {
								markError("Cannot assign to final field: " + targetType.getClassName() + "." + fieldName);
								cg.leaveExpression();
								return false;
							}
						}
					}
	            }
			}

			// Проверка совместимости типов
			if (!isCompatibleWith(scope, leftType, rightType)) {
				markError("Type mismatch: " + leftType.getName() + " " + operator + " " + rightType.getName());
				cg.leaveExpression();
				return false;
			}
			
			// Специфичные проверки операторов
			if (operator.isLogical()) {
				if (!leftType.isBoolean() || !rightType.isBoolean()) {
					markError("Logical operators require boolean operands");
					cg.leaveExpression();
					return false;
				}
				cg.leaveExpression();
				return true;
			}

			if (operator.isBitwise()) {
				if (!leftType.isInteger() || !rightType.isInteger()) {
					markError("Bitwise operators require integer operands");
					cg.leaveExpression();
					return false;
				}

				// Запрет смешивания разных целочисленных типов
				if (leftType != rightType) {
					markError("Bitwise operations require identical integer types");
					cg.leaveExpression();
					return false;
				}
				cg.leaveExpression();
				return true;
			}
			
			// Проверка деления на ноль (универсальная для всех типов)
			if ((Operator.DIV == operator || Operator.MOD == operator)) {
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
				else {
					// Для не-литералов предупреждаем о потенциальном делении на ноль
					if (rightType.isNumeric() && !rightType.isBoolean()) {
						//TODO constant folding...
						markWarning("Potential division by zero - runtime check required");
					}
				}
			}
			
			// Проверки для арифметических операций
			if (operator.isArithmetic()) {
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
						catch(SemanticException e) {
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
						catch(SemanticException e) {
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
		catch(SemanticException e) {markError(e); return false;}

		cg.leaveExpression();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		Object rl = depCodeGen(cg, leftExpr.getSymbol());
		Object rr = depCodeGen(cg, rightExpr.getSymbol());
		
		if(null == rl && null == rr) {
			if(rightExpr instanceof VarFieldExpression) {
				rr = (CGCellsScope)((AstHolder)rightExpr.getSymbol()).getNode().getCGScope();
				cg.cellsToAcc(cgScope.getParent(), (CGCellsScope)rr);
			}
			else if(leftExpr instanceof VarFieldExpression) {
				rl = (CGCellsScope)((AstHolder)leftExpr.getSymbol()).getNode().getCGScope();
				cg.cellsToAcc(cgScope.getParent(), (CGCellsScope)rl);
			}		
		}

		if(null == rl && null == rr) {
			throw new SemanticException("Both operands not used accum:" + toString());
		}	

		ExpressionNode noAccExpr = rightExpr;
		if(operator.isCommutative()) {
			if(null != rl && null != rr) { //TODO похоже ошибка, здесь я пытаюсь выяснить порядок генерации кода, но resId генерируется в postAnalyze!
				if(((CGScope)rl).getResId()<((CGScope)rr).getResId()) noAccExpr = leftExpr;
			}
			else if(null != rr) noAccExpr = leftExpr;
		}
		else {
			if(noAccExpr == leftExpr) {
				rl = (CGCellsScope)((AstHolder)leftExpr.getSymbol()).getNode().getCGScope();
				cg.cellsToAcc(cgScope.getParent(), (CGCellsScope)rl);
				noAccExpr = leftExpr;
			}
		}
		
		// Генерация кода для левого и правого операндов
		if(noAccExpr instanceof VarFieldExpression) {
			VarFieldExpression ve = (VarFieldExpression)noAccExpr;
			if(ve.getSymbol() instanceof AstHolder) {
				AstHolder ah = (AstHolder)ve.getSymbol();
				cg.cellsAction(cgScope.getParent(), (CGCellsScope)ah.getNode().getCGScope(), operator);
			}
			else {
				throw new SemanticException("Unsupported:" + ve.getSymbol());
			}
		}
		else if(noAccExpr instanceof LiteralExpression) {
			LiteralExpression le = (LiteralExpression)noAccExpr;
			cg.constAction(cgScope.getParent(), operator, le.getNumValue());
		}
		else if(noAccExpr instanceof MethodCallExpression) {
			MethodCallExpression mce = (MethodCallExpression)noAccExpr;
			int t=3434;
		}
		else {
			throw new SemanticException("Unsupported:" + rightExpr);
		}
		return null;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(leftExpr, rightExpr);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + leftExpr + ", " + operator + ", " + rightExpr;
	}
}