/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

public class VariableSymbol extends Symbol {
	private	Long	value;
	private	boolean	isConstant;
	
	public VariableSymbol(String name, long value, boolean isConstant) {
		super(name);
		this.value = value;
		this.isConstant = isConstant;
	}
	
	public long getValue()  {
		return value;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
}
