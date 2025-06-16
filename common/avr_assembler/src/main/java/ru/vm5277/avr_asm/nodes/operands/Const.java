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
		Long _value = Expression.getLong(expr, sp);
		if(null == _value) {
			throw new ParseException("Cannot resolve constant '" + expr + "'", sp);
		}

		long mask = (1<<bits)-1;
		if(Scope.STRICT_STRONG != Scope.getStrincLevel() && 0==min && _value>max) {
			long new_value = _value & mask;
			if(new_value<=max) {
				if(Scope.STRICT_NONE != Scope.getStrincLevel()) {
					mc.add(new WarningMessage("Constant value " + _value + " exceeds " + bits + "-bit range. Truncated to: " + new_value, sp));
				}
				_value = new_value;
			}
		}
		
		if(min>_value || max<_value) {
			mc.add(new ErrorMessage("Constant value out of range (" + _value + "), expected " + min + "≤ value <" + max, sp));
			_value &= mask;
		}
		value = _value;
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
