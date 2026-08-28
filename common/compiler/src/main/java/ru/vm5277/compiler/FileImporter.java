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
package ru.vm5277.compiler;

import ru.vm5277.common.lexer.Lexer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.tokens.Token;

public class FileImporter {
	private	final	Instance			inst;
	private	final	Path				runtimePath;
	private	final	Path				basePath;
	
	public FileImporter(Instance inst, Path runtimePath, Path basePath) {
		this.inst = inst;
		this.runtimePath = runtimePath;
		this.basePath = basePath;
	}

	public List<Token> importFile(String importPath, int tabSize) throws IOException {
		if(null==basePath) return null;
		// Проверка циклических зависимостей
		if(!inst.getImportedFiles().contains(importPath)) {
			inst.getImportedFiles().add(importPath);
		}

		List<Token> result = null;
		if(null!= runtimePath) {
			result = getTokens(runtimePath, importPath, tabSize);
			if(null!=result) return result;
		}

		if(null!=basePath) {
			result = getTokens(basePath, importPath, tabSize);
			if(null!=result) return result;
		}
		
		for(Path includePath : inst.getIncludePaths()) {
			result = getTokens(includePath, importPath, tabSize);
			if(null!=result) return result;
		}

		return null;
	}
	
	private List<Token> getTokens(Path dir, String filename, int tabSize) {
		try {
			File file = dir.resolve(filename).normalize().toFile();
			if(null!=file && file.exists()) {
				Lexer lexer = new Lexer(LexerType.J8B, file, null, tabSize, false);
				return lexer.getTokens();
			}
		}
		catch(IOException ignore)  {
		}
		return null;
	}
}