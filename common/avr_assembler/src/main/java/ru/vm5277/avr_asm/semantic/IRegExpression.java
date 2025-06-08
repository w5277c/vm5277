/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
08.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.common.exceptions.ParseException;

public class IRegExpression extends Expression {
	protected	int	id;
	protected	boolean	inc	= false;
	protected	boolean	dec	= false;

	public IRegExpression(int id, boolean isDec, boolean isInc) {
		this.id = id;
		this.dec = isDec;
		this.inc = isInc;
	}
	
	public IRegExpression(String ireg) throws ParseException {
		switch (ireg) {
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
				throw new ParseException("TODO ожидаем индексный регистр, получили " + ireg, sp);
		}
	}

	public int getId() {
		return id;
	}
	
	public boolean isDec() {
		return dec;
	}
	
	public boolean isInc() {
		return inc;
	}
	
	@Override
	public String toString() {
		return (dec ? "-" : "") + id + (inc ? "+" : "");
	}
}
