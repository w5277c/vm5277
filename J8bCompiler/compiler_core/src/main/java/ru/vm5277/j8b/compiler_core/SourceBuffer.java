/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core;

import ru.vm5277.j8b.compiler.common.SourcePosition;

public class SourceBuffer extends SourcePosition {
	private	String			src		= null;
	private	int				pos		= -1;

	public SourceBuffer(String src) {
		this.src = src;
		this.pos = 0;
		this.line = 1;
		this.column = 1;
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

	public void incLine() {
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
	
	// Фиксируем текущую позицию в отдельный SourcePosition 
	public SourcePosition snapSP() {
		return new SourcePosition(line, column);
	}
}