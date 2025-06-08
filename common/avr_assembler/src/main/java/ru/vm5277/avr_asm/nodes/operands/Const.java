/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.nodes.Node;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class Const {
	private	int		bits;
	private	long	value;

	public Const(Scope scope, SourcePosition sp, Expression expr, int min, int max, int bits) throws ParseException {
		this.bits = bits;
		value = Node.getNumValue(expr, sp);
		if(min>value || max<value) {
			throw new ParseException("TODO значение вне диапазона " + min + "<=" + value + "<" + max, sp);
		}
	}	

	public int getBits() {
		return bits;
	}
	
	public long getValue() {
		return value;
	}
}
