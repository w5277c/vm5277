/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.Delimiter;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.InfoMessage;
import ru.vm5277.common.messages.MessageContainer;

public class MessageNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		SourcePosition sp = tb.getSP();
		
		Expression expr = Expression.parse(tb, scope, mc);
		StringBuilder sb = new StringBuilder(expr.toString());
		while(tb.match(Delimiter.COMMA)) {
			tb.consume();
			expr = Expression.parse(tb, scope, mc);
			sb.append(expr.toString());
			if(tb.match(TokenType.EOF) || tb.match(TokenType.NEWLINE)) break;
		}
		mc.add(new InfoMessage(sb.toString(), sp));

		scope.list(".MESSAGE " + sb.toString());

		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
