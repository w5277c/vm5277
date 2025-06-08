/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.06.2025	konstantin@5277.ru		Взято из компилятора j8b
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Operator;
import ru.vm5277.common.messages.MessageContainer;


public class UnaryExpression extends Expression {
    private final Operator		operator;
    private final Expression	operand;
    
    public UnaryExpression(TokenBuffer tb, Scope scope, MessageContainer mc, Operator operator, Expression operand) {
		this.operator = operator;
        this.operand = operand;
    }
	
	public Operator getOperator() {
		return operator;
	}
	
	public Expression getOperand() {
		return operand;
	}
}