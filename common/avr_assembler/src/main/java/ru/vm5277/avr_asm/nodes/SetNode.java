/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.scope.VariableSymbol;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.Operator;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class SetNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		SourcePosition sp = tb.getSP();
		String name = ((String)Node.consumeToken(tb, TokenType.ID).getValue()).toLowerCase();
		Node.consumeToken(tb, Operator.ASSIGN);
		scope.setVariable(new VariableSymbol(name, Node.getNumValue(Expression.parse(tb, scope, mc), tb.getSP()), false), sp);
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
