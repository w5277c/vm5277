/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

public class SourceBuffer implements Cloneable {
	private	String	src		= null;
	private	int		pos		= -1;
	private	int		line	= 1;
	private	int		column	= 1;

	public SourceBuffer(String src) {
		this.src = src;
		this.pos = 0;
	}

	private SourceBuffer(int line, int column) {	//Для Error
		this.line = line;
		this.column = column;
	}

	public int getLine() {
		return line;
	}
	public int getColumn() {
		return column;
	}
	public int getPos() {
		return pos;
	}

	public void next() {
		pos++;
		column++;
	}
	public void next(int len) {
		pos+=len;
		column+=len;
	}
	public void nextLine() {
		line++;
		column=1;
	}
	
	public void incPos() {
		pos++;
	}
	public void incColumn() {
		column++;
	}
	
	public String getSource() {
		return src;
	}
	
	@Override
	public SourceBuffer clone() {
		return new SourceBuffer(line, column);
	}
	
	public boolean hasNext() {
		return pos < src.length();
	}
	public boolean hasNext(int offset) {
		return pos+offset < src.length();
	}

	public char getChar() {
		return src.charAt(pos);
	}
	public char getChar(int offset) {
		return src.charAt(pos+offset);
	}
	
	@Override
	public String toString() {
		return "[" + line + ":" + column + "]";
	}
}