/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.tokens.enums.Delimiter;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class ClassNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			String			name;
	private			List<String>	interfaces	= new ArrayList<>();
	private	final	BlockNode		body;
	
	public ClassNode(TokenBuffer tb, Set<Keyword> modifiers) {
		super(tb);
		
		this.modifiers = modifiers;
		
		// 1. Парсинг заголовка класса
        tb.consume(TokenType.OOP, Keyword.CLASS);	// Пропуск class токена
		this.name = (String)tb.consume(TokenType.ID).getValue();

        // 2. Парсинг интерфейсов (если есть)
        if (tb.match(Keyword.IMPLEMENTS) || tb.match(Delimiter.COLON)) {
            tb.consume();
			while(true) {
				interfaces.add((String)tb.consume(TokenType.ID).getValue());
				if (!tb.match(Delimiter.COMMA)) break;
				tb.consume();
			}
        }

        // 3. Парсинг тела класса
		this.body = new BlockNode(tb);
	}
}
