/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class DoWhileNode extends AstNode {
	private	final	ExpressionNode	condition;

	public DoWhileNode(TokenBuffer tb) {
		super(tb);

		tb.consume();
		if(tb.match(Delimiter.LEFT_BRACE)) {
			try {
				tb.getLoopStack().add(this);
				blocks.add(new BlockNode(tb));
			}
			finally {
				tb.getLoopStack().remove(this);
			}
		}
		else blocks.add(new BlockNode(tb, parseStatement()));

		tb.consume(TokenType.COMMAND, Keyword.WHILE);
		tb.consume(Delimiter.LEFT_PAREN);

		this.condition = new ExpressionParser(tb).parse();
		tb.consume(Delimiter.RIGHT_PAREN);
		tb.consume(Delimiter.SEMICOLON);

		if (tb.match(Keyword.ELSE)) {
			tb.consume();
			blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));
		}
	}

	public ExpressionNode getCondition() {
		return condition;
	}

	public BlockNode getBody() {
		return blocks.get(0);
	}

	public BlockNode getElseBlock() {
		return blocks.size() > 1 ? blocks.get(1) : null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("do ");
		sb.append(getBody());
		sb.append(" while (");
		sb.append(condition);
		sb.append(");");

		if (getElseBlock() != null) {
			sb.append(" else ").append(getElseBlock());
		}

		return sb.toString();
	}
}