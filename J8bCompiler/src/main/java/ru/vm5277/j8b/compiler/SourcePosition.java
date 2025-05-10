/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

public class SourcePosition implements Cloneable {
	protected	int		line	= -1;
	protected	int		column	= -1;

	public SourcePosition() {
	}

	SourcePosition(int line, int column) {
		this.line = line;
		this.column = column;
	}

	public int getLine() {
		return line;
	}
	public int getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return "[" + line + ":" + column + "]";
	}
}