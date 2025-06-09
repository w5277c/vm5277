/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
10.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class IOReg {
	private	int	id;

	public IOReg(Scope scope, SourcePosition sp, Expression expr) throws ParseException {
		id = Expression.getLong(expr, sp).intValue();
		if(0>id || 0x1f<id) {
			throw new ParseException("TODO ожидаем IO регистр(0-31), получили " + id, sp);
		}
	}
	
	public int getId() {
		return id;
	}
}
