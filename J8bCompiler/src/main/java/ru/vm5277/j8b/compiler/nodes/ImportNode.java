/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class ImportNode extends AstNode {
	private	boolean	isStatic;
	private	String	importPath;
	private	String	alias;

	public ImportNode(TokenBuffer tb) {
        super(tb);
		
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
	public String toString() {
		return getClass().getSimpleName() + ": " + importPath;
	}
}
