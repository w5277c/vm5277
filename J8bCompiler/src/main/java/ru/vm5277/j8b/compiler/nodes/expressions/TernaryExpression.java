/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class TernaryExpression extends ExpressionNode {
	private final ExpressionNode condition;
	private final ExpressionNode trueExpr;
	private final ExpressionNode falseExpr;

	public TernaryExpression(TokenBuffer tb, ExpressionNode condition, ExpressionNode trueExpr, ExpressionNode falseExpr) {
		super(tb);

		this.condition = condition;
		this.trueExpr = trueExpr;
		this.falseExpr = falseExpr;
	}

	// Геттеры
	public ExpressionNode getCondition() {
		return condition;
	}

	public ExpressionNode getTrueExpr() {
		return trueExpr;
	}

	public ExpressionNode getFalseExpr() {
		return falseExpr;
	}

	@Override
	public String toString() {
		return condition + " ? " + trueExpr + " : " + falseExpr;
	}

	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public <T> T accept(ExpressionVisitor<T> visitor) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}
}