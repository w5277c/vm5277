/*
 * Copyright 2026 konstantin@5277.ru
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

package ru.vm5277.common.lexer;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SourceBuffer extends SourcePosition implements Closeable {
	private	InputStreamReader		isr;
	private	ArrayList<Character>	queue	= new ArrayList<>(1024);
	private	int						tabSize	= 0x04;
	
	public SourceBuffer(File sourceFile, int tabSize) throws FileNotFoundException {
		super(sourceFile);
		
		isr = new InputStreamReader(new FileInputStream(sourceFile), StandardCharsets.UTF_8);
		this.tabSize = tabSize;
	}
	
	public SourceBuffer(InputStream is, int tabSize) {
		super(null);
		
		this.isr = new InputStreamReader(is, StandardCharsets.UTF_8);
		this.tabSize = tabSize;
	}

	public SourceBuffer(String sourceCode, int tabSize) {
		super(null);
		
		this.isr = new InputStreamReader(new ByteArrayInputStream(sourceCode.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		this.tabSize = tabSize;
	}

	public char next() {
		try {
			char result;

			if(!queue.isEmpty()) {
				result = queue.remove(0);
			}
			else {
				int b = isr.read();
				result = (-1==b) ? '\0' : (char)b;
			}

			column++;
			pos++;
			return result;
		}
		catch(IOException ex) {
			return '\0';
		}
		
	}

	public void next(int count) {
		for (int i = 0; i < count; i++) {
			next();
		}
	}
	
	public char peek() {
		try {
			if(!queue.isEmpty()) {
				return queue.get(0);
			}

			int b = isr.read();
			if(b==-1) return '\0';

			char c = (char) b;
			queue.add(c);

			return c;
		}
		catch(IOException ex) {
			return '\0';
		}
	}

	public char peek(int offset) {
		if(!available(offset+1)) {
			return '\0';
		}

		return queue.get(offset);
	}
	
	public void gotNewLine() {
		line++;
		column = 1;
	}
	
	public void gotTab() {
		column+=(tabSize-1);
	}
	
	public boolean available(int size) {
		try {
			int needed = size-queue.size();

			for(int i=0; i<needed; i++) {
				int b = isr.read();
				if(-1==b) return false;
				queue.add((char)b);
			}
			return true;
		}
		catch(IOException ex) {
			return false;
		}
	}

	public boolean available() {
		try {
			if(queue.isEmpty()) {
				int b = isr.read();
				if(-1==b) return false;
				queue.add((char)b);
				return true;
			}
			return true;
		}
		catch(IOException ex) {
			return false;
		}
	}

	public boolean consumeIfEqual(String token) {
		if(!available(token.length())) {
			return false;
		}

		for(int i=0; i<token.length(); i++) {
			if(token.charAt(i)!=queue.get(i)) {
				return false;
			}
		}

		for(int i=0; i<token.length(); i++) {
			queue.remove(0);
		}
		pos+=token.length();

		return true;
	}
	
	public String peekString(int size) {
		if(!available(size)) return null;
		
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<size; i++) {
			sb.append(queue.get(i));
		}
		
		return sb.toString();
	}
	
	// Фиксируем текущую позицию в отдельный SourcePosition 
	public SourcePosition snapSP() {
		return new SourcePosition(sourceFile, line, column, pos);
	}

	public int getLineQnt() {
		return line;
	}
	
	@Override
	public void close() throws IOException {
		isr.close();
	}
}
