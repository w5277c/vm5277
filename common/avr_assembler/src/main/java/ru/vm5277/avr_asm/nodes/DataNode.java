/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.nio.charset.StandardCharsets;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class DataNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc, int valueSize) throws ParseException {
		while(true) {
			byte[] tmp = null;
			Expression expr = Expression.parse(tb, scope, mc);
			if(expr instanceof LiteralExpression && ((LiteralExpression)expr).getValue() instanceof String) {
				tmp = ((String)((LiteralExpression)expr).getValue()).getBytes(StandardCharsets.US_ASCII);
			}
			else {
				long value = Node.getNumValue(expr, tb.getSP());
				if(value >= (1<<(valueSize*8))) {
					throw new ParseException("TODO значение больше, чем указанный тип данных:" + value + ", size:" + valueSize, tb.getSP());
				}
				tmp = new byte[valueSize];
				for(int i=0; i<valueSize; i++) {
					tmp[i] = (byte)(value & 0xff);
					value >>>= 8;
				}
			}
			
			scope.getCurrentSegment().getCurrentBlock().write(tmp, tmp.length);			
			if(tb.match(TokenType.NEWLINE) || tb.match(TokenType.EOF)) break;
			Node.consumeToken(tb, Delimiter.COMMA);
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
