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

package ru.vm5277.compiler.nodes;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.compiler.semantic.CIScope;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.tokens.Token;
import ru.vm5277.compiler.ASTParser;
import ru.vm5277.compiler.FileImporter;
import ru.vm5277.compiler.Instance;

public abstract class ObjectTypeNode extends AstNode {
	protected			List<ObjectTypeNode>				imported; // Импорты в области видимости этого объекта
	protected			CIScope								ciScope;
	protected			Map<String, AnnotationParamNode>	annotations;
	protected			Set<Keyword>						modifiers;
	protected			String								name;
	protected			String								classPath;
	protected			List<String>						impl			= new ArrayList<>();
	protected			boolean								isInner;
	protected			boolean								isRtosDriver;
	
	public ObjectTypeNode(	Instance inst, TokenBuffer tb, Map<String, AnnotationParamNode> annotations, Set<Keyword> modifiers, String classPath,
							List<ObjectTypeNode> imported)																			throws CompileException {
		super(inst, tb);
		
		this.imported = imported;
		this.annotations = annotations;
		this.modifiers = modifiers;
		this.classPath = classPath;
		
		// Парсинг заголовка класса
        consumeToken(tb);	// Пропуск class токена
		try {
			this.name = (String)consumeToken(tb, TokenType.IDENTIFIER).getValue();
		}
		catch(CompileException e) {markFirstError(e);} // ошибка в имени, оставляем null
	}
	
	
	public void resolveImportFromCurrentDir(Instance inst, File sourceFile, String className) {
		if(null==sourceFile) return;
		
		// Проверяем в импортах
		for(ObjectTypeNode node : imported) {
			if(node.getName().equals(className)) {
				return;
			}
		}
		// Если в импортах не нашли - проверяем текущую директорию
		Path basePath = Paths.get(sourceFile.getAbsolutePath()).getParent();
		FileImporter fileImporter = new FileImporter(inst, null, basePath);
		try {
			List<Token> importedTokens = fileImporter.importFile(className + ".j8b", inst.getTabSize());
			if(null!= importedTokens && !importedTokens.isEmpty()) {
				// Рекурсивный парсинг импортированного файла
				ASTParser importedParser = new ASTParser(inst, basePath);
				importedParser.parse(importedTokens);
				if(null!=importedParser.getClazz()) {
					imported.add(importedParser.getClazz());
				}
			}
		}
		catch(Exception ex) {
			markError(ex.getMessage());
		}
	}

	public String getName() {
		return name;
	}
	
	public CIScope getScope() {
		return ciScope;
	}

	public String getFullName() {
		return null == classPath ? name : classPath + "." + name;
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	public boolean isStatic() {
		return modifiers.contains(J8BKeyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(J8BKeyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(J8BKeyword.PUBLIC);
	}
	public boolean isNative() {
		return modifiers.contains(J8BKeyword.NATIVE);
	}

	public boolean isImplemented(String ifaceName) {
		return impl.contains(ifaceName);
	}
	public List<String> getImpements() {
		return impl;
	}
	
	
	public List<ObjectTypeNode> getImported() {
		return imported;
	}
	
	public Map<String, AnnotationParamNode> getAnnotations() {
		return annotations;
	}
	
	public abstract AstNode getBody();
	
	public boolean isRTOSDriver() {
		return isRtosDriver;
	}
}
