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

public class AReg extends Reg {
	public AReg(Scope scope, SourcePosition sp, Expression expr) throws ParseException {
		super(scope, sp, expr);
		
		if(24!=id && 26!=id && 28!=id && 30!=id) {
			throw new ParseException("TODO ожидаем r24,XH,YH,ZH регистр, получили " + id, sp);
		}
	}
}
