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
	public void setMacroOffset(SourcePosition callSP, String macroName) {
		this.macroName = macroName;
		this.macroLine = line;

		this.sourceFile = callSP.getSourceFile();
		this.line = callSP.getLine();
		this.column = callSP.getColumn();
	}

	public int getColumn() {
		return column;
	}

	public File getSourceFile() {
		return sourceFile;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(line + ":" + column);
		if(null != macroName) {
			sb.append("|").append(macroName.toUpperCase()).append(":").append(macroLine);
		}
		sb.append(" ");
		if(null != sourceFile) sb.append(sourceFile.getAbsolutePath());
		sb.append("\t");
		return sb.toString();
		
	}
}