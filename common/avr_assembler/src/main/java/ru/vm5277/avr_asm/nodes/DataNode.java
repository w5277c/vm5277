/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.DatatypeConverter;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.avr_asm.Delimiter;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class DataNode {
	public static void parse(TokenBuffer tb, Scope scope, MessageContainer mc, int valueSize) throws ParseException {
		SourcePosition sp = tb.getSP();
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			while(true) {
				SourcePosition _sp = tb.getSP();
				byte[] tmp = null;
				Expression expr = Expression.parse(tb, scope, mc);
				if(expr instanceof LiteralExpression && ((LiteralExpression)expr).getValue() instanceof String) {
					tmp = ((String)((LiteralExpression)expr).getValue()).getBytes(StandardCharsets.US_ASCII);
				}
				else {
					Long value = Expression.getLong(expr, tb.getSP());
					if(null == value) {
						tb.skipLine();
						throw new ParseException("TODO не могу разрезолвить '" + expr + "'", _sp);
					}
					if(value >= (1<<(valueSize*8))) {
						tb.skipLine();
						throw new ParseException("TODO значение больше, чем указанный тип данных:" + value + ", size:" + valueSize, _sp);
					}
					tmp = new byte[valueSize];
					for(int i=0; i<valueSize; i++) {
						tmp[i] = (byte)(value & 0xff);
						value >>>= 8;
					}
				}

				baos.write(tmp);
				
				if(tb.match(TokenType.NEWLINE) || tb.match(TokenType.EOF)) break;
				Node.consumeToken(tb, Delimiter.COMMA);
			}
			
			if(0 != baos.size()) {
				if(0 != (baos.size()&0x01)) {
					if(Scope.STRICT_STRONG == Scope.getStrincLevel()) {
						throw new ParseException("TODO размер flash данных не четный", sp);
					}
					else if(Scope.STRICT_LIGHT == Scope.getStrincLevel()) {
						mc.add(new WarningMessage("TODO размер flash данных не четный", sp));
					}
					baos.write(0x00);
				}
				int wSize = baos.size()/2;
				scope.getCSeg().getCurrentBlock().write(baos.toByteArray(), wSize);
				scope.getCSeg().movePC(wSize);
			
				if(scope.isListEnabled()) {
					String text = "";
					try {text = " #" + new String(baos.toByteArray(), "ASCII").replaceAll("\\r", "\\\\r").replaceAll("\\n", "\\\\n");}catch(Exception e) {}
					scope.list(".DB 0x" + DatatypeConverter.printHexBinary(baos.toByteArray()) + text);
				}
			}
			Node.consumeToken(tb, TokenType.NEWLINE);
		}
		catch(Exception e) {
			throw new ParseException(e.getMessage(), sp);
		}
	}
}
