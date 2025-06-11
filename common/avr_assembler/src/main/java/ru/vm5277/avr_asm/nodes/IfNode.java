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
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class IfNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException, CriticalParseException {
		Long value = 0x00l;
		if(scope.getIncludeSymbol().isBlockSkip()) {
			try{Expression.parse(tb, scope, mc);} catch(ParseException e) {};
			//TODO должно быть везде, где вычисляются выражения. Так как переменные и метки могут отсутствовать
		}
		else {
			value = Expression.getLong(Expression.parse(tb, scope, mc), tb.getSP());
		}
		scope.getIncludeSymbol().blockStart(0x01!=value);
		
		scope.list(".IF " + " # " + (0 != value));
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
