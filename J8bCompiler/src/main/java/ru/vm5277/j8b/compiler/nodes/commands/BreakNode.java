/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.nodes.*;

public class BreakNode extends AstNode {
    private	String	label;
	
	public BreakNode(TokenBuffer tb) {
        super(tb);
        
        consumeToken(tb);
        
		if(tb.match(TokenType.ID)) {
			try {label = consumeToken(tb, TokenType.ID).toString();}catch(ParseException e) {markFirstError(e);};
		}
		
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
        
		AstNode node = tb.getLoopStack().peek();
		if (null == node || !(node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode)) {
			markFirstError(tb.error("'break' can only be used inside loop statements"));
        }
    }

	public String getLabel() {
		return label;
	}
	
	@Override
    public String toString() {
        return "break" + (null!=label ? " " + label : "");
    }
}