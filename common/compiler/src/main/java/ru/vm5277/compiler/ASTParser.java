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
import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.common.messages.ErrorMessage;

public class ASTParser extends AstNode {
	private	final	FileImporter			fileImporter;
	private			List<AstNode>			imports			= new ArrayList<>();
	private			List<ObjectTypeNode>	autoImported	= new ArrayList<>();
	private			List<ObjectTypeNode>	imported		= new ArrayList<>();
	private			ObjectTypeNode			objTypeNode;
	
	public ASTParser(Instance inst, Path basePath, List<Token> tokens) throws IOException {
		this(inst, basePath, tokens, false, false);
	}
	public ASTParser(Instance inst, Path basePath, List<Token> tokens, boolean firsLaunch) throws IOException {
		this(inst, basePath, tokens, firsLaunch, false);
	}
	public ASTParser(Instance inst, Path basePath, List<Token> tokens, boolean firsLaunch, boolean syntheticMode) throws IOException {
		this.fileImporter = new FileImporter(inst.getRuntimePath(), basePath, inst.getMessageContainer());
		
		if(tokens.isEmpty()) return;
		
        // Автоматический импорт из runtime/autoimport.cfg и загрузка не исключаемых методов для кодогенерации
        if(firsLaunch && null!=inst.getRuntimePath()) {
			importAutoConfiguredClasses(inst, basePath);
			loadPersistMethods(inst.getRuntimePath(), basePath);
		}

		tb = new TokenBuffer(tokens.listIterator());

		String classPath = null;
		File sourceFile = tb.getSP().getSourceFile();
		if(null!=sourceFile && sourceFile.toPath().startsWith(inst.getRuntimePath())) {
			Path tmp = inst.getRuntimePath().relativize(sourceFile.toPath()).getParent();
			classPath = tmp.toString().replaceAll("[/\\\\]", ".");
		}

		// Обработка импортов		
		while (tb.match(J8BKeyword.IMPORT) && !tb.match(TokenType.EOF)) {
			ImportNode importNode = new ImportNode(inst, tb);
			imports.add(importNode);
			
			// Загрузка импортируемого файла
			List<Token> importedTokens = fileImporter.importFile(importNode.getImportFilePath(), inst.getTabSize());
			if(null==importedTokens) {
				inst.getMessageContainer().add(new ErrorMessage("Imported file not found: " + importNode.getImportFilePath(), sp));
			}
			else if(!importedTokens.isEmpty()) {
				// Рекурсивный парсинг импортированного файла
				ASTParser importedParser = new ASTParser(inst, basePath, importedTokens);
				if(null!=importedParser.getClazz()) {
					imported.add(importedParser.getClazz());
				}
			}
		}
		
		Set<Keyword> modifiers = collectModifiers(tb);
		if(tb.match(TokenType.OOP, J8BKeyword.INTERFACE)) {
			try {
				objTypeNode = new InterfaceNode(inst, tb, modifiers, classPath, imported);
			}
			catch(CompileException e) {
				markError(e);
				return;
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
		else if(tb.match(TokenType.OOP, J8BKeyword.EXCEPTION)) {
			try {
				objTypeNode = new ExceptionNode(inst, tb, modifiers, imported);
			}
			catch(CompileException e) {
				markError(e);
				return;
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
		else if(tb.match(TokenType.OOP, J8BKeyword.CLASS)) {
			try {
				objTypeNode = new ClassNode(inst, tb, modifiers, false, imported); //TODO понадобится class path
			}
			catch(CompileException e) {
				markError(e);
				return;
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
		// Добавляем префикс и постфикс для synthericMode
		if(syntheticMode && null==objTypeNode && !tb.match(TokenType.EOF)) {
			try {
				Lexer lexer = new Lexer(LexerType.J8B, "class Main{\npublic static void main() {\n", false, inst.getTabSize());
				lexer.getTokens().addAll(tokens);
				tokens = lexer.getTokens();
				lexer = new Lexer(LexerType.J8B, "\n}\n}", false, inst.getTabSize());
				tokens.addAll(lexer.getTokens());
				
				List<Token> filtered = new ArrayList<>();
				for(Token token : tokens) {
					if(TokenType.EOF!=token.getType()) {
						filtered.add(token);
					}
				}
				filtered.add(new Token(TokenType.EOF));
				
				tb = new TokenBuffer(filtered.listIterator());
				modifiers = collectModifiers(tb);
				objTypeNode = new ClassNode(inst, tb, modifiers, false, imported);
			}
			catch(CompileException e) {
				markError(e);
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
	private void importAutoConfiguredClasses(Instance inst, Path basePath) throws IOException {
		Path autoImportConfig = inst.getRuntimePath().resolve("autoimport.cfg");

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
					ImportNode importNode = new ImportNode(inst, tb, importPath);
					imports.add(importNode);

					// Преобразуем import path в путь к файлу
					String fileRuntimePath = importPath.replace('.', File.separatorChar) + ".j8b";

					// Загружаем и парсим файл
					List<Token> importedTokens = fileImporter.importFile(fileRuntimePath, inst.getTabSize());
					if(null==importedTokens) {
						inst.getMessageContainer().add(new ErrorMessage("Imported file not found: " + fileRuntimePath, sp));
					}
					else {
						if(!importedTokens.isEmpty()) {
							ASTParser importedParser = new ASTParser(inst, basePath, importedTokens);
							if(null!=importedParser.getClazz()) {
								autoImported.add(importedParser.getClazz());
							}
						}
						else {
							markWarning("Auto-import file not found or empty: " + fileRuntimePath);
						}
					}
				}
			}
		}
	}
	
	private void loadPersistMethods(Path runtimePath, Path basePath) throws IOException {
		Path persistConfig = runtimePath.resolve("persist.cfg");

		if(!persistConfig.toFile().exists()) {
			markWarning("Persist configuration not found: " + persistConfig);
			return;
		}

		try (BufferedReader br = new BufferedReader(new FileReader(persistConfig.toFile()))) {
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
					if(importPath.replaceAll("^([a-zA-Z_][a-zA-Z0-9_]*\\.)+[a-zA-Z_][a-zA-Z0-9_]*\\(.*\\)$", "").isEmpty()) {
						persists.add(importPath);
					}
					else {
						markError("Illegal persist declaration: " + importPath + ", expected format: package.Class.method(args)");
					}
				}
			}
		}
	}
}

