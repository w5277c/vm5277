/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.avr_asm.scope.CodeSegment;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class OrgNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		SourcePosition sp = tb.getSP();
		CodeSegment cSeg = scope.getCSeg();
		Expression expr = Expression.parse(tb, scope, mc);
		Long value = Expression.getLong(expr, sp);
		if(null == value) {
			tb.skipLine();
			throw new ParseException("Cannot resolve constant '" + expr + "'", sp);
		}
		if(0>value || value>cSeg.getWSize()) throw new ParseException("Address 0x" + Long.toHexString(value) + " exceeds flash memory size", sp);
		
		scope.getCSeg().setPC(value.intValue());
		
		scope.list(".ORG " + value.intValue());
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
