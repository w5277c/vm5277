/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.nodes.*;

public class ContinueNode extends AstNode {
    public ContinueNode(TokenBuffer tb) {
        super(tb);
        
        tb.consume();
        tb.consume(Delimiter.SEMICOLON);
        
		AstNode node = tb.getLoopStack().peek();
		if (null == node || !(node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode)) {
            throw new ParseError("'continue' can only be used inside loop statements", tb.getSB());
        }
    }

	@Override
    public String toString() {
        return getClass().getSimpleName();
    }
}