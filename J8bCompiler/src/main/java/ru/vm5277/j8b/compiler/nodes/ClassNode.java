/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class ClassNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			String			name;
	private			String			parentClassName;
	private			List<String>	interfaces	= new ArrayList<>();
	
	public ClassNode(TokenBuffer tb, Set<Keyword> modifiers, String parentClassName) {
		super(tb);
		
		this.modifiers = modifiers;
		this.parentClassName = parentClassName;
		
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
		blocks.add(new BlockNode(tb, name));
	}
	
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return null == parentClassName ? name : parentClassName + "." + name;
	}
	
	public BlockNode getBody() {
		return blocks.get(0);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + name + ", " + interfaces;
	}

}
