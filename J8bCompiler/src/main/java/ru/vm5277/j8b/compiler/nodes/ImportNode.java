/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.tokens.enums.Delimiter;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class ImportNode extends AstNode {
	private	final	boolean	isStatic;
	private	final	String	importPath;
	private	final	String	alias;

	public ImportNode(TokenBuffer tb) {
        super(tb);
		
		// Пропуск import токена
		tb.consume(TokenType.KEYWORD, Keyword.IMPORT);
        
		if (Keyword.STATIC == tb.current().getValue()) {
            tb.consume();
            this.isStatic = true;
        }
		else {
			this.isStatic = false;
		}

		StringBuilder path = new StringBuilder();
        
        // Парсим первый идентификатор
        Token first = tb.consume(TokenType.ID);
        path.append(first.getValue());
        
        // Парсим оставшиеся части пути (через точки)
        while (tb.match(TokenType.DELIMITER) && Delimiter.DOT == tb.current().getValue()) {
            tb.consume();
            Token part = tb.consume(TokenType.ID);
            path.append(".").append(part.getValue());
        }
        
        this.importPath = path.toString();

		if (tb.match(Keyword.AS)) {
			tb.consume();
            this.alias = (String)tb.consume(TokenType.ID).getValue();
        }
		else {
			this.alias = null;
		}

		tb.consume(Delimiter.SEMICOLON); // Завершаем точкой с запятой
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
}
