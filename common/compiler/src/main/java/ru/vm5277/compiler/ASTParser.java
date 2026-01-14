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

import ru.vm5277.common.lexer.TokenType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.ExceptionNode;
import ru.vm5277.compiler.nodes.ImportNode;
import ru.vm5277.compiler.nodes.InterfaceNode;
import ru.vm5277.compiler.nodes.ObjectTypeNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.tokens.Token;

public class ASTParser extends AstNode {
	private	final	FileImporter			fileImporter;
	private			List<AstNode>			imports			= new ArrayList<>();
	private			List<ObjectTypeNode>	autoImported	= new ArrayList<>();
	private			List<ObjectTypeNode>	imported		= new ArrayList<>();
	private			ObjectTypeNode			objTypeNode;
	
	public ASTParser(Path runtimePath, Path basePath, List<Token> tokens, MessageContainer mc, int tabSize) throws IOException {
		this(runtimePath, basePath, tokens, mc, false, tabSize);
	}
	public ASTParser(Path runtimePath, Path basePath, List<Token> tokens, MessageContainer mc, boolean firsLaunch, int tabSize) throws IOException {
		this.fileImporter = new FileImporter(runtimePath, basePath, mc);
		this.mc = mc;
		
		if(tokens.isEmpty()) return;
		
		tb = new TokenBuffer(tokens.listIterator());
		
        // Автоматический импорт из runtime/autoimport.cfg
        if(firsLaunch && null!=runtimePath) {
			importAutoConfiguredClasses(runtimePath, basePath, tabSize);
		}
		
		// Обработка импортов		
		while (tb.match(J8BKeyword.IMPORT) && !tb.match(TokenType.EOF)) {
			ImportNode importNode = new ImportNode(tb, mc);
			imports.add(importNode);
			
			// Загрузка импортируемого файла
			List<Token> importedTokens = fileImporter.importFile(importNode.getImportFilePath(), tabSize);
			if(!importedTokens.isEmpty()) {
				// Рекурсивный парсинг импортированного файла
				ASTParser importedParser = new ASTParser(runtimePath, basePath, importedTokens, mc, tabSize);
				if(null!=importedParser.getClazz()) {
					imported.add(importedParser.getClazz());
				}
			}
		}
		
		Set<Keyword> modifiers = collectModifiers(tb);
		if(tb.match(TokenType.OOP, J8BKeyword.INTERFACE)) {
			try {
				objTypeNode = new InterfaceNode(tb, mc, modifiers, null, imported);
			}
			catch(CompileException e) {
				markError(e);
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
		else if(tb.match(TokenType.OOP, J8BKeyword.EXCEPTION)) {
			try {
				objTypeNode = new ExceptionNode(tb, mc, modifiers, imported);
			}
			catch(CompileException e) {
				markError(e);
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
		else if(tb.match(TokenType.OOP, J8BKeyword.CLASS)) {
			try {
				objTypeNode = new ClassNode(tb, mc, modifiers, false, imported);
			}
			catch(CompileException e) {
				markError(e);
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
	}
	
	public List<AstNode> getImports() {
		return imports;
	}

	public List<ObjectTypeNode> getAutoImported() {
		return autoImported;
	}
	
	public ObjectTypeNode getClazz() {
		return objTypeNode;
	}

	public TokenBuffer getTB() {
		return tb;
	}

	@Override
	public List<AstNode> getChildren() {
		return Arrays.asList(objTypeNode);
	}
	
	//TODO реализовать кеширование на базе сериализации и хеш сумм файлов
	private void importAutoConfiguredClasses(Path runtimePath, Path basePath, int tabSize) throws IOException {
		Path autoImportConfig = runtimePath.resolve("autoimport.cfg");

		if(!autoImportConfig.toFile().exists()) {
			markWarning("Auto-import configuration not found: " + autoImportConfig);
			return;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(autoImportConfig.toFile()))) {
			while(true) {
				String importPath = br.readLine();
				if(null==importPath) break;
				// Убираем комментарии и лишние пробелы
				int pos = importPath.indexOf('#');
				if(pos>=0) {
					importPath = importPath.substring(0, pos);
				}

				importPath = importPath.trim();

				if(!importPath.isEmpty()) {
					// Создаем виртуальный ImportNode для логирования
					ImportNode importNode = new ImportNode(tb, mc, importPath);
					imports.add(importNode);

					// Преобразуем import path в путь к файлу
					String filePath = importPath.replace('.', File.separatorChar) + ".j8b";

					// Загружаем и парсим файл
					List<Token> importedTokens = fileImporter.importFile(filePath, tabSize);
					if(!importedTokens.isEmpty()) {
						ASTParser importedParser = new ASTParser(runtimePath, basePath, importedTokens, mc, tabSize);
						if(null!=importedParser.getClazz()) {
							autoImported.add(importedParser.getClazz());
						}
					}
					else {
						markWarning("Auto-import file not found or empty: " + filePath);
					}
				}
			}
		}
	}
}

