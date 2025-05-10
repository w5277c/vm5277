/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.enums.Operator;
import static ru.vm5277.j8b.compiler.enums.Operator.DIV;
import static ru.vm5277.j8b.compiler.enums.Operator.MINUS;
import static ru.vm5277.j8b.compiler.enums.Operator.MOD;
import static ru.vm5277.j8b.compiler.enums.Operator.MULT;
import static ru.vm5277.j8b.compiler.enums.Operator.PLUS;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class BinaryExpression extends ExpressionNode {
    private final ExpressionNode	left;
    private final Operator			operator;
    private final ExpressionNode	right;
    
    public BinaryExpression(TokenBuffer tb, ExpressionNode left, Operator operator, ExpressionNode right) {
        super(tb);
        
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
		if (!leftType.isCompatibleWith(rightType)) {
			throw new SemanticException("Type mismatch in binary operation: " + leftType + " " + operator + " " + rightType);
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

			VarType leftType = left.getType(scope);
			VarType rightType = right.getType(scope);
			
			
			// Проверка совместимости типов
			if (!leftType.isCompatibleWith(rightType)) {
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
				
				if(leftType.getSize() < rightType.getSize()) {
					markError(	"Implicit type conversion from " + leftType.getName() + " to " + rightType.getName() + " is prohibited. Use explicit cast.");
					return false;
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
	public String toString() {
		return getClass().getSimpleName() + ": " + left + ", " + operator + ", " + right;
	}
}