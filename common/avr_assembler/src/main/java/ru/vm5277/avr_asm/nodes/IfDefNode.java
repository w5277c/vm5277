/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class IfDefNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException, CriticalParseException {
		SourcePosition sp = tb.getSP();
		String id = (String)Node.consumeToken(tb,TokenType.ID).getValue();
		scope.getIncludeSymbol().blockStart(null == scope.resolveVariable(id) && null == scope.resolveLabel(id), sp);

		scope.list(".IFDEF " + id + " # " + !scope.getIncludeSymbol().isBlockSkip());

		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
