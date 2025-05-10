/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
27.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.enums.Operator;

public class Term {
    private	Operator		operator;
    private	ExpressionNode	node;
    private	boolean			isPositive;
    
    public Term(Operator op, ExpressionNode node, boolean isPositive) {
        this.operator = op;
        this.node = node;
        this.isPositive = isPositive;
    }

	public boolean isNumber() {
		return node instanceof LiteralExpression && ((LiteralExpression)node).getValue() instanceof Number;
	}

	public Operator getOperator() {
		return operator;
	}
	
	public ExpressionNode getNode() {
		return node;
	}
	
	public boolean isPositive() {
		return isPositive;
	}
}