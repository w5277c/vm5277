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
import java.util.List;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.tokens.Token;

public class ImportNode extends AstNode {
	private	boolean	isStatic;
	private	String	importStr;
	private	String	alias;

	public ImportNode(TokenBuffer tb, MessageContainer mc) {
        super(tb, mc);
		
		try {
			// Пропуск import токена
			consumeToken(tb);

			if (tb.match(Keyword.STATIC)) {
				consumeToken(tb);
				this.isStatic = true;
			}
			else {
				this.isStatic = false;
			}

			StringBuilder path = new StringBuilder();

			// Парсим первый идентификатор
			Token first = consumeToken(tb, TokenType.ID);
			path.append(first.getValue());

			// Парсим оставшиеся части пути (через точки)
			while (tb.match(TokenType.DELIMITER) && Delimiter.DOT == tb.current().getValue()) {
				consumeToken(tb);
				Token part = consumeToken(tb, TokenType.ID);
				path.append(".").append(part.getValue());
			}

			this.importStr = path.toString();

			if (tb.match(Keyword.AS)) {
				consumeToken(tb);
				this.alias = (String)consumeToken(tb, TokenType.ID).getValue();
			}
			else {
				this.alias = null;
			}
			//Попытка потребить ';'
			try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
		}
		catch(CompileException e) {
			// Что-то пошло не так, фиксируем ошибку
			markFirstError(e);
			// Пропускаем до конца выражения
			tb.skip(Delimiter.SEMICOLON);
		}
    }

	public String getImportFilePath() {
		return (importStr.replace(".", File.separator) + ".j8b");
	}
	
    // Геттеры
    public String getImport() {
        return importStr;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getAlias() {
        return alias;
    }

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + importStr;
	}

	@Override
	public boolean preAnalyze() {
		if (importStr == null || importStr.isEmpty()) markError("Import path cannot be empty");
		else {
			// Проверяем статический импорт
			if (isStatic) {
				// Можно добавить дополнительные проверки для статического импорта
				if (!importStr.contains(".")) 	markError("Static import must specify full class path and member");
			}
		}
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope) scope;

			// Регистрируем импорт в классе
			try {
				if (isStatic) {
					classScope.addStaticImport(importStr, alias);
				}
				else {
					classScope.addImport(importStr, alias);
				}
			}
			catch(CompileException e) {markError(e);}
			return true;
		}
		markError("Import declarations must be at the beginning of the file, before any class members");
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		if(scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;
			if(isStatic) {
				// Проверяем существование статического члена
				if(!classScope.checkStaticImportExists(importStr)) {
					markError("Static import not found: " + importStr);
				}
			}
			else {
				// Проверяем существование класса
				if(null==classScope.resolveCI(importStr, false)) {
					markError("Imported class not found: " + importStr);
				}
			}
		}
		return true;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}
