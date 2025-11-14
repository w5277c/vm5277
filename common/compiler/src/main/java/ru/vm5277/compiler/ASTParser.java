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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.ImportNode;
import ru.vm5277.compiler.nodes.InterfaceNode;
import ru.vm5277.compiler.nodes.ObjectTypeNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.tokens.Token;

public class ASTParser extends AstNode {
	private	final	FileImporter		fileImporter;
	private			List<AstNode>		imports			= new ArrayList<>();
	private			List<ObjectTypeNode>importedClasses	= new ArrayList<>();
	private			ObjectTypeNode		objTypeNode;

	public ASTParser(Path runtimePath, Path basePath, List<Token> tokens, MessageContainer mc) throws IOException {
		this.fileImporter = new FileImporter(runtimePath, basePath, mc);
		this.mc = mc;
		
		if(tokens.isEmpty()) return;
		
		tb = new TokenBuffer(tokens.listIterator());
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
		if (tb.match(TokenType.OOP, Keyword.INTERFACE)) {
			try {
				objTypeNode = new InterfaceNode(tb, mc, modifiers, null, importedClasses);
			}
			catch(CompileException e) {
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
		else if(tb.match(TokenType.OOP, Keyword.CLASS)) {
			try {
				objTypeNode = new ClassNode(tb, mc, modifiers, false, importedClasses);
			}
			catch(CompileException e) {
				// Парсинг прерван (дальнейший парсинг файла бессмыслен)
			}
		}
	}
	
	public List<AstNode> getImports() {
		return imports;
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
}

