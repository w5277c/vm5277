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
import java.nio.file.Path;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.cg.items.CGIContainer;

public class ExcsThrowPoint {
	private	Integer			id;
	private	SourcePosition	sp;
	private	String			signature;
	private	boolean			inUse;

	private	CGIContainer	cont7b;
	private	CGIContainer	cont15b;
	
	public ExcsThrowPoint(int id, SourcePosition sp, String signature) {
		this.id = id;
		this.sp = sp;
		this.signature = signature;
	}

	public Integer getId() {
		return id;
	}

	public SourcePosition getSp() {
		return sp;
	}

	public void apply(int id) {
		this.id = id;
	}

	public CGIContainer makeContainer(CGIContainer cont7b, CGIContainer cont15b) {
		this.cont7b = cont7b;
		this.cont15b = cont15b;
		
		CGIContainer result = new CGIContainer();
		result.append(cont7b);
		result.append(cont15b);
		return result;
	}
	
	public void chooseContainer(boolean is7Bit) {
		if(null!=cont15b) {
			if(is7Bit) {
				cont15b.disable();
			}
			else {
				cont7b.disable();
			}
		}
	}
	
	public String toString(Path sourcePath) {
		if(null==sp || null==id || null==sp.getSourceFile()) return "";
		
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append(" ");
		
		String filePathStr = sp.getSourceFile().getPath().toString();
		String sourcePathStr = sourcePath.toString();
		int pos = sourcePathStr.lastIndexOf(File.separator);
		if(-1!=pos) {
			sourcePathStr = sourcePathStr.substring(0, pos+1);
			if(filePathStr.startsWith(sourcePathStr)) {
				filePathStr = filePathStr.substring(sourcePathStr.length());
			}
		}
		sb.append(filePathStr);
		
		sb.append(":");
		sb.append(sp.getLine());
		sb.append(" ");
		sb.append(signature);
		
		return sb.toString();
	}
}
