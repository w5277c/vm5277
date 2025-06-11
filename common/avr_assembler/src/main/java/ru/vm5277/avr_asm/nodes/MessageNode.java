/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.InfoMessage;
import ru.vm5277.common.messages.MessageContainer;

public class MessageNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		SourcePosition sp = tb.getSP();
		String text = (String)Node.consumeToken(tb, TokenType.STRING).getValue();
		mc.add(new InfoMessage(text, sp));

		scope.list(".MESSAGE " + text);

		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
