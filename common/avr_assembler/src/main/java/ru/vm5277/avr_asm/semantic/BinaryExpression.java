/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Operator;
import ru.vm5277.common.messages.MessageContainer;

public class BinaryExpression extends Expression {
	private	Expression	leftExpr;
	private	Operator	operator;
	private	Expression	rightExpr;
	
	public BinaryExpression(TokenBuffer tb, Scope scope, MessageContainer mc, Expression left, Operator operator, Expression right) {
		this.leftExpr = left;
		this.operator = operator;
		this.rightExpr = right;
	}
	
	@Override
	public String toString() {
		return leftExpr.toString() + operator.getSymbol() + rightExpr.toString();
	}
}
