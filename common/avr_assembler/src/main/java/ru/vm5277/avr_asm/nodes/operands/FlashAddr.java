/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class FlashAddr extends Const {
	public FlashAddr(MessageContainer mc, Scope scope, SourcePosition sp, Expression expr, int min, int max, int bits, int addr) throws ParseException {
		this.bits = bits;
		value = Expression.getLong(expr, sp) - addr;
		
		if(min>value || max<value) {
			mc.add(new WarningMessage("значение вне диапазона " + min + "<=" + value + "<" + max, sp));
			value = 0;
//throw new ParseException("TODO значение вне диапазона " + min + "<=" + value + "<" + max, sp);
		}
	}	
}
