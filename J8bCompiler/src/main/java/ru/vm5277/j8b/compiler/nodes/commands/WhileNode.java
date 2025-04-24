/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.enums.Delimiter;

public class WhileNode extends AstNode {
	private	final	ExpressionNode	condition;
	
	public WhileNode(TokenBuffer tb) {
		super(tb);
		
		tb.consume(); // Пропускаем "while"
        tb.consume(Delimiter.LEFT_PAREN);
        this.condition = new ExpressionParser(tb).parse();
        tb.consume(Delimiter.RIGHT_PAREN);

		blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, "") : new BlockNode(tb, parseStatement()));
	}
	
	public ExpressionNode getCondition() {
		return condition;
	}

	public BlockNode getBody() {
		return blocks.get(0);
	}
}
