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
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class Const {
	protected	int		bits;
	protected	long	value;

	protected Const() {
	}
	
	public Const(MessageContainer mc, Scope scope, SourcePosition sp, Expression expr, int min, int max, int bits) throws ParseException {
		this.bits = bits;
		value = Expression.getLong(expr, sp);
		long mask = (1<<bits)-1;
		if(Scope.STRICT_ERROR != Scope.getStrincLevel() && 0==min && value>max) {
			long new_value = value & mask;
			if(new_value<=max) {
				if(Scope.STRICT_IGNORE != Scope.getStrincLevel()) {
					mc.add(new WarningMessage("TODO константа " + value + " превышает размер, обрезано до " + bits + " бит:" + new_value, sp));
				}
				value = new_value;
			}
		}
		
		if(min>value || max<value) {
			mc.add(new ErrorMessage("TODO значение вне диапазона " + min + "<=" + value + "<" + max, sp));
			value &= mask;
		}
	}	

	public int getBits() {
		return bits;
	}
	
	public long getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return Long.toString(value);
	}
}
