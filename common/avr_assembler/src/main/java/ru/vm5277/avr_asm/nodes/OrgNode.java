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
		int wAddr = Expression.getLong(Expression.parse(tb, scope, mc), tb.getSP()).intValue();
		if(0>wAddr || wAddr>cSeg.getWSize()) throw new ParseException("TODO адрес за пределами flash памяти", sp);
		
		scope.getCSeg().setPC(wAddr);
		
		scope.list(".ORG " + wAddr);
		
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
