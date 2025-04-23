/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.tokens.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class ParameterNode extends AstNode {
	private	final	Keyword	type;
    private	final	String	name;
	
	public ParameterNode(TokenBuffer tb) {
		super(tb);
		
        this.type = (Keyword)tb.consume(TokenType.TYPE).getValue();
        this.name = (String)tb.consume(TokenType.ID).getValue();
	}

}
