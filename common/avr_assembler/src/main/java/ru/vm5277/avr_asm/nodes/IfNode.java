/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;

public class IfNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException, CriticalParseException {
		Long value = 0x00l;
		SourcePosition sp = tb.getSP();
		
		Expression expr = Expression.parse(tb, scope, mc);
		if(!scope.getIncludeSymbol().isBlockSkip()) {
			Long _value = Expression.getLong(expr, sp);
			if(null == _value) {
				mc.add(new ErrorMessage("Could not resolve condition: '" + expr + "'", sp));
			}
			else {
				value = _value;
			}
		}
		scope.getIncludeSymbol().blockStart(0x01!=value, sp);
		
		scope.list(".IF " + " # " + (0 != value));
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
