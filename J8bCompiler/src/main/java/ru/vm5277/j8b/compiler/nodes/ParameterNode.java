/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ParameterNode extends AstNode {
	private	final	VarType	type;
    private	final	String	name;
	
	public ParameterNode(TokenBuffer tb) {
		super(tb);
		
        this.type = VarType.fromKeyword((Keyword)tb.consume(TokenType.TYPE).getValue());
        this.name = (String)tb.consume(TokenType.ID).getValue();
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
}
