/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes.operands;

import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.semantic.IRegExpression;
import ru.vm5277.avr_asm.semantic.IdExpression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.exceptions.ParseException;

public class Reg {
	protected	int	id;
	protected	boolean	inc	= false;
	protected	boolean	dec	= false;
	
	protected Reg(int id) {
		this.id = id;
	}
	
	public Reg(Scope scope, SourcePosition sp, Expression expr) throws ParseException {
		if(expr instanceof LiteralExpression) {
			String str = (String)((LiteralExpression)expr).getValue();
			switch (str) {
				case "x": id=26; break;
				case "x+": id=26; inc=true; break;
				case "-x": id=26; dec=true; break;
				case "y": id=28; break;
				case "y+": id=28; inc=true; break;
				case "-y": id=28; dec=true; break;
				case "z": id=30; break;
				case "z+": id=30; inc=true; break;
				case "-z": id=30; dec=true; break;
				default:
					throw new ParseException("TODO ожидаем регистр, получили " + expr, sp);
			}
		}
		else if(expr instanceof IdExpression) {
			String str = (String)((IdExpression)expr).getId();
			Byte result = scope.resolveReg(str);
			if(null == result) {
				throw new ParseException("Unable to resolve register '" + str + "'", sp); 
			}
			id = result;
		}
		else if(expr instanceof IRegExpression) {
			id = ((IRegExpression)expr).getId();
		}
		else {
			throw new ParseException("TODO ожидаем регистр, получили " + expr, sp);
		}
		if(0x00>id || 0x1f<id) {
			throw new ParseException("TODO ожидаем регистр, получили " + id, sp);
		}
	}
	public Reg(byte id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return Integer.toString(id);
	}
}
