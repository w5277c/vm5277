/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.SemanticError;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class UnaryExpression extends ExpressionNode {
    private final Operator		 operator;
    private final ExpressionNode operand;
    
    public UnaryExpression(TokenBuffer tb, Operator operator, ExpressionNode operand) {
        super(tb);
        this.operator = operator;
        this.operand = operand;
    }
    
	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) {
		VarType operandType = operand.semanticAnalyze(symbolTable);

		// Проверка допустимости операции для типа
		if (!isUnaryOperationValid(operandType, operator)) {
			throw new SemanticError(String.format("Invalid unary operation %s for type %s", operator, operandType), line, column);
		}

		return operandType; // Для большинства унарных операций тип сохраняется
	}
    
	@Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
	
	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getOperand() {
		return operand;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + operator + " " + operand;
	}
}