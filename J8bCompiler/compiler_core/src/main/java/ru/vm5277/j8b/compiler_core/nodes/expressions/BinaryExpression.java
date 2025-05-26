/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.Operand;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.common.enums.Operator;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;
import ru.vm5277.j8b.compiler_core.semantic.Symbol;

public class BinaryExpression extends ExpressionNode {
    private	final	ExpressionNode	left;
    private	final	Operator		operator;
    private	final	ExpressionNode	right;
	private			VarType			leftType;
	private			VarType			rightType;

    public BinaryExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode left, Operator operator, ExpressionNode right) {
        super(tb, mc);
        
		this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
	public ExpressionNode getLeft() {
		return left;
	}
	
	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getRight() {
		return right;
	}

	@Override
	public VarType getType(Scope scope) throws SemanticException {
		// Получаем типы операндов
		VarType leftType = left.getType(scope);
		VarType rightType = right.getType(scope);

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
		if (null == left) {
			markError("Left operand is missing in binary expression");
			return false;
		}
		if (null == right) {
			markError("Right operand is missing in binary expression");
			return false;
		}
		
		// Рекурсивный анализ операндов
		if (!left.preAnalyze()) {
			return false; // Ошибка уже помечена в left
		}
		if (!right.preAnalyze()) {
			return false; // Ошибка уже помечена в right
		}

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			// Анализ операндов
			if (!left.postAnalyze(scope) || !right.postAnalyze(scope)) return false;

			leftType = left.getType(scope);
			rightType = right.getType(scope);

			if(operator.isAssignment()) {
				if(left instanceof VariableExpression) {
					VariableExpression varExpr = (VariableExpression) left;
					Symbol symbol = scope.resolve(varExpr.getValue());
					if(null != symbol && symbol.isFinal()) {
						markError("Cannot assign to final variable: " + varExpr.getValue());
						return false;
					}
				}
				else if (left instanceof MemberAccessExpression) {
					// Для полей классов проверяем final (если нужно)
					MemberAccessExpression memberExpr = (MemberAccessExpression) left;
					ExpressionNode target = memberExpr.getTarget();
					String fieldName = memberExpr.getMemberName();

					// Получаем тип объекта
					VarType targetType = target.getType(scope);

					if (targetType.isClassType()) {
						// Ищем класс в scope
						ClassScope classScope = scope.getThis().resolveClass(targetType.getClassName());
						if (null != classScope) {
							Symbol field = classScope.getFields().get(fieldName);
							if (null != field && field.isFinal()) {
								markError("Cannot assign to final field: " + targetType.getClassName() + "." + fieldName);
								return false;
							}
						}
					}
	            }
			}

			// Проверка совместимости типов
			if (!isCompatibleWith(scope, leftType, rightType)) {
				markError("Type mismatch: " + leftType.getName() + " " + operator + " " + rightType.getName());
				return false;
			}
			
			// Специфичные проверки операторов
			if (operator.isLogical()) {
				if (!leftType.isBoolean() || !rightType.isBoolean()) {
					markError("Logical operators require boolean operands");
					return false;
				}
				return true;
			}

			if (operator.isBitwise()) {
				if (!leftType.isInteger() || !rightType.isInteger()) {
					markError("Bitwise operators require integer operands");
					return false;
				}

				// Запрет смешивания разных целочисленных типов
				if (leftType != rightType) {
					markError("Bitwise operations require identical integer types");
					return false;
				}
				return true;
			}
			
			// Проверка деления на ноль (универсальная для всех типов)
			if ((Operator.DIV == operator || Operator.MOD == operator)) {
				if (right instanceof LiteralExpression) {
					Object val = ((LiteralExpression)right).getValue();
					boolean isZero = false;

					if (val instanceof Double) isZero = (Double)val == 0.0;
					else if (val instanceof Number) isZero = ((Number)val).longValue() == 0L;

					if (isZero) {
						markError("Division by zero");
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
				if (left instanceof LiteralExpression && right instanceof LiteralExpression) {
					LiteralExpression leftLE = (LiteralExpression)left;
					LiteralExpression rightLE = (LiteralExpression)right;

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
							return false;
						}
					}
				}
				
				// Особые проверки для fixed-point
				if (leftType == VarType.FIXED || rightType == VarType.FIXED) {
					if (operator == Operator.MOD) {
						markError("Modulo operation is not supported for FIXED type");
						return false;
					}
				}
			}
		}
		catch(SemanticException e) {markError(e); return false;}

		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		// Генерация кода для левого и правого операндов
		left.codeGen(cg);
		Operand leftOp = cg.getAcc();
		right.codeGen(cg);
		//Operand rightOp = cg.getAcc();

		// Обработка операторов присваивания (=, +=, -=, и т.д.)
		if (operator.isAssignment()) {
			// Для простого присваивания (=) генерируем сохранение значения
			if (operator == Operator.ASSIGN) {
				// Если левый операнд — переменная, генерируем store
				if (left instanceof VariableExpression) {
					VariableExpression varExpr = (VariableExpression) left;
					//TODO cg.emitStoreVariable(varExpr.getValue(), leftType);
					cg.storeAcc(varExpr.getSymbol().getRuntimeId());
				}
				// Если левый операнд — доступ к полю (x.y), генерируем putfield
				else if (left instanceof MemberAccessExpression) {
					MemberAccessExpression memberExpr = (MemberAccessExpression) left;
					//TODO cg.emitFieldStore(memberExpr.getTarget().getType(cg.getCurrentScope()).getClassName(),memberExpr.getMemberName(),leftType);
				}
			}
			// Для составных присваиваний (+=, -=, ...) генерируем соответствующий оператор
			else {
/*				cg.emitBinaryOp(arithmeticOp, leftType, rightType);
				// Затем сохраняем результат обратно
				if (left instanceof VariableExpression) {
					VariableExpression varExpr = (VariableExpression) left;
					cg.emitStoreVariable(varExpr.getValue(), leftType);
				} else if (left instanceof MemberAccessExpression) {
					MemberAccessExpression memberExpr = (MemberAccessExpression) left;
					cg.emitFieldStore(
						memberExpr.getTarget().getType(cg.getCurrentScope()).getClassName(),
						memberExpr.getMemberName(),
						leftType
					);
				}*/
			}
		}
		// Обработка арифметических, логических и битовых операций
		else {
			//cg.emitBinaryOp(operator, leftType, rightType);
		}
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + left + ", " + operator + ", " + right;
	}
}