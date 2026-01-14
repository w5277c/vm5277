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
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.tokens.Token;

public class FileImporter {
	private	final	List<String>		importedFiles	= new ArrayList<>();
	private	final	Path				runtimePath;
	private	final	Path				basePath;
	private	final	MessageContainer	mc;

	public FileImporter(Path runtimePath, Path basePath, MessageContainer mc) {
		this.runtimePath = runtimePath;
		this.basePath = basePath;
		
		this.mc = mc;
	}

	public List<Token> importFile(String importPath, int tabSize) throws IOException {
		// Проверка циклических зависимостей
		if (importedFiles.contains(importPath)) {
			mc.add(new WarningMessage("Circular import detected: " + importPath, null));
		}
		else {
			importedFiles.add(importPath);
		}

		File file = runtimePath.resolve(importPath).normalize().toFile();
		if (!file.exists()) {
			file = basePath.resolve(importPath).normalize().toFile();
			if(!file.exists()) {
				mc.add(new ErrorMessage("Imported file not found: " + importPath, null));
				file = null;
			}
		}
		if(null != file) {
			Lexer lexer = new Lexer(LexerType.J8B, file, null, tabSize, false);
			return lexer.getTokens();
		}
		return new ArrayList<>();
	}
}