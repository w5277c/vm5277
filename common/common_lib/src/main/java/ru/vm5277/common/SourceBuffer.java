/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.common;

import java.io.File;

public class SourceBuffer extends SourcePosition {
	private	String			src		= null;
	private	int				pos		= -1;

	public SourceBuffer(File sourceFile, String src) {
		super(sourceFile);
		
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
	
	public void nextTab(int tabSize) {
		pos++;
		column = ((column/tabSize)+1)*tabSize;
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
	
	public int getLineQnt() {
		return line;
	}
	
	@Override
	public String toString() {
		return "[" + line + ":" + column + "]";
	}
	
	// Фиксируем текущую позицию в отдельный SourcePosition 
	public SourcePosition snapSP() {
		return new SourcePosition(sourceFile, line, column);
	}
}