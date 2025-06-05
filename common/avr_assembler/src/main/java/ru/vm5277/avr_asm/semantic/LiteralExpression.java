/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

public class LiteralExpression extends Expression {
	private	final	Object	value;
	
	public LiteralExpression(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value.toString();
	}
}
