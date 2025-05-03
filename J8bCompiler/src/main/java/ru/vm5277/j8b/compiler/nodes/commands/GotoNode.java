/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.nodes.*;

public class GotoNode extends AstNode {
    private	String	label;
	
	public GotoNode(TokenBuffer tb) {
        super(tb);
        
        consumeToken(tb);
		try {label = consumeToken(tb, TokenType.ID).toString();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
    }

	public String getLabel() {
		return label;
	}
	
	@Override
    public String toString() {
        return getClass().getSimpleName();
    }
}