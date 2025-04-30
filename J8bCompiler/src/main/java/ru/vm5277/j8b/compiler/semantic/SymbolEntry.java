/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
24.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.VarType;

public class SymbolEntry {
	final	VarType			type;
	final	boolean			isMutable;
	final	SourceBuffer	sb;

	SymbolEntry(VarType type, boolean isMutable, SourceBuffer sb) {
		this.type = type;
		this.isMutable = isMutable;
		this.sb = sb;
	}

	public VarType getType() {
		return type;
	}

	public boolean isMutable() {
		return isMutable;
	}
}
