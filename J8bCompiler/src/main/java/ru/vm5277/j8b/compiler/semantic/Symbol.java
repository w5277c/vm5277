/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import ru.vm5277.j8b.compiler.enums.VarType;

public class Symbol {
	protected	String	name;
	protected	VarType	type;
	protected	boolean	isStatic;
	protected	boolean	isFinal;

	protected Symbol(String name) {
		this.name = name;
	}
	
	public Symbol(String name, VarType type) {
		this.name = name;
		this.type = type;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
	}

	public String getName() {
		return name;
	}
	
	public VarType getType() {
		return type;
	}
	public void setType(VarType type) {
		this.type = type;
	}
	
	public boolean isFinal() {
		return isFinal;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
}