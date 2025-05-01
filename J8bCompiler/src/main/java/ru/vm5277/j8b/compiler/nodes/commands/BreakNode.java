/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.tokens.Token;

public class BreakNode extends AstNode {
    private	String	label;
	
	public BreakNode(TokenBuffer tb) {
        super(tb);
        
        tb.consume();
        
		if(tb.match(TokenType.ID)) {
			Token nameToken = tb.consume(TokenType.ID);
			label = nameToken.toString();
		}
		
		tb.consume(Delimiter.SEMICOLON);
        
		AstNode node = tb.getLoopStack().peek();
		if (null == node || !(node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode)) {
            throw new ParseError("'break' can only be used inside loop statements", sp);
        }
    }

	public String getLabel() {
		return label;
	}
	
	@Override
    public String toString() {
        return getClass().getSimpleName();
    }
}