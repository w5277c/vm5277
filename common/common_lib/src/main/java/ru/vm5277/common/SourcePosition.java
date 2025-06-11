/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common;

import java.io.File;

public class SourcePosition implements Cloneable {
	private		String	macroName	= null;
	private		int		macroLine	= 0;
	protected	File	sourceFile;
	protected	int		line		= -1;
	protected	int		column		= -1;

	public SourcePosition(File sourceFile) {
		this.sourceFile = sourceFile;
	}

	public SourcePosition(File sourceFile, int line, int column) {
		this.sourceFile = sourceFile;
		this.line = line;
		this.column = column;
	}

	public int getLine() {
		return line;
	}
	public void setMacroOffset(String macroName, int macroLine) {
		this.macroName = macroName;
		this.macroLine = macroLine;
	}
	public void updateLine(int macroFirstLine) {
		line -= (macroFirstLine+1);
	}

	public int getColumn() {
		return column;
	}

	public File getSourceFile() {
		return sourceFile;
	}
	
	@Override
	public String toString() {
		if(null == macroName) {
			return line + ":" + column + " " + (null == sourceFile ? "" : sourceFile.getAbsolutePath()) + "\t";
		}
		return	macroLine + ":" + column + "|MACRO:" + macroName.toUpperCase() + ":" + line + " " +
				(null == sourceFile ? "" : sourceFile.getAbsolutePath()) + "\t";
	}
}