/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;

public class ParameterNode extends AstNode {
	private	final	VarType	type;
    private	final	String	name;
	
	public ParameterNode(TokenBuffer tb) throws ParseException {
		super(tb);
		
		this.type = checkPrimtiveType();
		this.name = (String)consumeToken(tb, TokenType.ID).getValue();
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + type + ", " + name;
	}
}
