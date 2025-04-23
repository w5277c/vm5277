/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

public class ParseError extends RuntimeException {
	private	final	int	line;
	private	final	int	column;

	public ParseError(String message, int line, int column) {
		super(message + " at line " + line + ", column " + column);
		this.line = line;
		this.column = column;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column;
	}
}
