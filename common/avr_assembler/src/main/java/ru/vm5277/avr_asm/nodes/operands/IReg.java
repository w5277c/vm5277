/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
09.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class IReg extends Reg {
	
	public IReg(SourcePosition sp, int id) throws ParseException {
		super(id);
		
		if(26!=id && 28!=id && 30!=id) {
			throw new ParseException("TODO ожидаем XH,YH,ZH регистр, получили " + id, sp);
		}
	}
}
