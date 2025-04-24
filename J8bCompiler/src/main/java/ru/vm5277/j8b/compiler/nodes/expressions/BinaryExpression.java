/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.SemanticError;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

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
    
	// Для визитора (понадобится при кодогенерации)
	@Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
	
	public ExpressionNode getLeft() {
		return left;
	}
	
	public Operator getOperastor() {
		return operator;
	}
	
	public ExpressionNode getRight() {
		return right;
	}

	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) {
		VarType leftType = left.semanticAnalyze(symbolTable);
		VarType rightType = right.semanticAnalyze(symbolTable);

		// Проверка совместимости типов
		if (!leftType.isCompatibleWith(rightType)) {
			throw new SemanticError(String.format("Type mismatch in binary operation: %s %s %s", leftType, operator, rightType), line, column);
		}

		// Определение типа результата операции
		if (operator.isComparison()) return VarType.BOOL;
		if (operator.isLogical()) return VarType.BOOL;
		return leftType.getSize() >= rightType.getSize() ? leftType : rightType;
	}
}