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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.ImportNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.tokens.Token;

public class ASTParser extends AstNode {
	private	final	FileImporter		fileImporter;
	private			List<AstNode>		imports			= new ArrayList<>();
	private			List<ClassNode>		importedClasses	= new ArrayList<>();
	private			ClassNode			classNode;

	public ASTParser(Path runtimePath, Path basePath, List<Token> tokens, MessageContainer mc) throws IOException {
		this.fileImporter = new FileImporter(runtimePath, basePath, mc);
		this.mc = mc;
		
		if(tokens.isEmpty()) return;
		
		tb = new TokenBuffer(tokens.iterator());
		// Обработка импортов		
		while (tb.match(Keyword.IMPORT) && !tb.match(TokenType.EOF)) {
			ImportNode importNode = new ImportNode(tb, mc);
			imports.add(importNode);
			
			// Загрузка импортируемого файла
			List<Token> importedTokens = fileImporter.importFile(importNode.getImportFilePath());
			if (!importedTokens.isEmpty()) {
				// Рекурсивный парсинг импортированного файла
				ASTParser importedParser = new ASTParser(runtimePath, basePath, importedTokens, mc);
				if (null != importedParser.getClazz()) {
					importedClasses.add(importedParser.getClazz());
				}
			}
		}
		
		Set<Keyword> modifiers = collectModifiers(tb);
		if(tb.match(TokenType.OOP, Keyword.CLASS)) {
			try {
				classNode = new ClassNode(tb, mc, modifiers, null, importedClasses);
			}
			catch(ParseException e) {
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
	}
	
	public List<AstNode> getImports() {
		return imports;
	}

	public ClassNode getClazz() {
		return classNode;
	}

	public List<ClassNode> getImportedClasses() {
		return importedClasses;
	}
	
	@Override
	public String getNodeType() {
		return "root";
	}
	
	public TokenBuffer getTB() {
		return tb;
	}
}

