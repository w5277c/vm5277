/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class Reg {
	protected	int	id;

	public Reg(Scope scope, SourcePosition sp, Expression expr) throws ParseException {
		if(expr instanceof LiteralExpression) {
			Byte result = scope.resolveReg((String)((LiteralExpression)expr).getValue());
			if(null==result || 0x00>result || 0x1f<result) {
				throw new ParseException("TODO ожидаем регистр, получили " + result, sp);
			}
			id = result;
		}
		throw new ParseException("TODO ожидаем регистр, получили " + expr, sp);
	}
	public Reg(byte id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
