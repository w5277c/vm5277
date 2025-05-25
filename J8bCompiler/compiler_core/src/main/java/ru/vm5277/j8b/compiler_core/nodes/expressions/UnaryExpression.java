/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.common.enums.Operator;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;
import ru.vm5277.j8b.compiler_core.semantic.Symbol;

public class UnaryExpression extends ExpressionNode {
    private final Operator		 operator;
    private final ExpressionNode operand;
    
    public UnaryExpression(TokenBuffer tb, MessageContainer mc, Operator operator, ExpressionNode operand) {
        super(tb, mc);
        
		this.operator = operator;
        this.operand = operand;
    }
    
	@Override
	public VarType getType(Scope scope) throws SemanticException {
		VarType operandType = operand.getType(scope);

		switch (operator) {
			case NOT: return VarType.BOOL;
			case PRE_INC:
			case PRE_DEC:
			case POST_INC:
			case POST_DEC:
			case PLUS:
			case MINUS: return VarType.FIXED;
			case BIT_NOT:
			default: return operandType;
		}
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			// Проверяем операнд
			if (!operand.postAnalyze(scope)) {
				return false;
			}

			VarType operandType = operand.getType(scope);

			// Проверяем допустимость оператора для типа
			switch (operator) {
				case PLUS:
					if (!operandType.isNumeric() && VarType.CSTR != operandType) {
						markError("Unary " + operator + " requires numeric or string type");
						return false;
					}
					break;
					
				case MINUS:
					if (!operandType.isNumeric()) {
						markError("Unary " + operator + " requires numeric type");
						return false;
					}
					break;

				case BIT_NOT:
					if (!operandType.isInteger()) {
						markError("Bitwise ~ requires integer type");
						return false;
					}
					break;

				case NOT:
					if (operandType != VarType.BOOL) {
						markError("Logical ! requires boolean type");
						return false;
					}
					break;

				case PRE_INC:
				case PRE_DEC:
				case POST_INC:
				case POST_DEC:
					if (!operandType.isNumeric()) {
						markError("Increment/decrement requires numeric type");
						return false;
					}
					if (!(operand instanceof VariableExpression)) {
						markError("Can only increment/decrement variables");
						return false;
					}
					// Дополнительная проверка на изменяемость переменной
					if (isFinalVariable((VariableExpression)operand, scope)) {
						markError("Cannot modify final variable");
						return false;
					}
					break;

				default:
					markError("Unsupported unary operator: " + operator);
					return false;
			}

			return true;
		} catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}
	
	private boolean isFinalVariable(VariableExpression var, Scope scope) {
		Symbol symbol = scope.resolve(var.getValue());
		if(null == symbol) {
			ClassScope classScope = scope.getThis().resolveClass(var.getValue());
			if(null != classScope) {
				symbol = new Symbol(var.getValue(), VarType.CLASS, false, false);
			}
		}
		return symbol != null && symbol.isFinal();
	}
	
	
	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getOperand() {
		return operand;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		// Генерация кода для операнда (например, переменной или другого выражения)
		operand.codeGen(cg);

		cg.emitUnary(operator);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + operator + " " + operand;
	}
}