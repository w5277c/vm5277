/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.Operator;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class DefNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		String name = (String)Node.consumeToken(tb, TokenType.ID).getValue();
		Node.consumeToken(tb, Operator.ASSIGN);
		String str = (String)Node.consumeToken(tb, TokenType.ID).getValue();
		Byte reg = scope.resolveReg(str);
		if(null != reg) {
			scope.addRegAlias(name, reg, tb.getSP());
		}
		else {
			throw new ParseException("TODO не верно указан регистр:" + str, tb.getSP());
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
