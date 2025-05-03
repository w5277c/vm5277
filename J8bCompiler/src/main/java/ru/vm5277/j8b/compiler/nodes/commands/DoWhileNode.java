/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;

public class DoWhileNode extends AstNode {
	private	ExpressionNode	condition;

	public DoWhileNode(TokenBuffer tb) {
		super(tb);

		consumeToken(tb);
		// Тело цикла
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		try {
			consumeToken(tb, TokenType.COMMAND, Keyword.WHILE);
			consumeToken(tb, Delimiter.LEFT_PAREN);
			this.condition = new ExpressionNode(tb).parse();
			consumeToken(tb, Delimiter.RIGHT_PAREN);
		}
		catch(ParseException e) {
			markFirstError(e);
		}

		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	public ExpressionNode getCondition() {
		return condition;
	}

	public BlockNode getBody() {
		return blocks.isEmpty() ? null : blocks.get(0);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("do ");
		sb.append(getBody());
		sb.append(" while (");
		sb.append(condition);
		sb.append(");");
		return sb.toString();
	}
}