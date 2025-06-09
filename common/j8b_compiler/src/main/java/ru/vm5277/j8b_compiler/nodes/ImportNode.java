/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes;

import java.io.File;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.j8b_compiler.Delimiter;
import ru.vm5277.j8b_compiler.Keyword;
import ru.vm5277.j8b_compiler.TokenType;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.semantic.ClassScope;
import ru.vm5277.j8b_compiler.semantic.Scope;
import ru.vm5277.j8b_compiler.tokens.Token;

public class ImportNode extends AstNode {
	private	boolean	isStatic;
	private	String	importPath;
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

			this.importPath = path.toString();

			if (tb.match(Keyword.AS)) {
				consumeToken(tb);
				this.alias = (String)consumeToken(tb, TokenType.ID).getValue();
			}
			else {
				this.alias = null;
			}
			//Попытка потребить ';'
			try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
		}
		catch(ParseException e) {
			// Что-то пошло не так, фиксируем ошибку
			markFirstError(e);
			// Пропускаем до конца выражения
			tb.skip(Delimiter.SEMICOLON);
		}
    }

	public String getImportFilePath() {
		return importPath.replace(".", File.separator) + ".j8b";
	}
	
    // Геттеры
    public String getImportPath() {
        return importPath;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getAlias() {
        return alias;
    }

	@Override
	public String getNodeType() {
		return "import";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + importPath;
	}

	@Override
	public boolean preAnalyze() {
		if (importPath == null || importPath.isEmpty()) markError("Import path cannot be empty");
		else {
			// Проверяем статический импорт
			if (isStatic) {
				// Можно добавить дополнительные проверки для статического импорта
				if (!importPath.contains(".")) 	markError("Static import must specify full class path and member");
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
					classScope.addStaticImport(importPath, alias);
				}
				else {
					classScope.addImport(importPath, alias);
				}
			}
			catch(SemanticException e) {markError(e);}
			return true;
		}
		markError("Import declarations must be at the beginning of the file, before any class members");
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		if (scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;
			if (isStatic) {
				// Проверяем существование статического члена
				if (!classScope.checkStaticImportExists(importPath)) {
					markError("Static import not found: " + importPath);
				}
			} else {
				// Проверяем существование класса
				if (classScope.resolveClass(importPath) == null) {
					markError("Imported class not found: " + importPath);
				}
			}
		}
		return true;
	}
}
