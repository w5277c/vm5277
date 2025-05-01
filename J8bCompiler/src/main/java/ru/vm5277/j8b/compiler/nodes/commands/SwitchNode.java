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
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.tokens.TNumber;
import ru.vm5277.j8b.compiler.tokens.Token;

public class SwitchNode extends AstNode {
	public static class Case {
		private	final	long		from;
		private	final	long		to;
		private	final	BlockNode	block;

		public Case(long from, long to, BlockNode block) {
			this.from = from;
			this.to = to;
			this.block = block;
		}
		
		public long getFrom() {
			return from;
		}
		
		public long getTo() {
			return to;
		}
		
		public BlockNode getBlock() {
			return block;
		}
	}

	private	final	ExpressionNode	expression;
	private	final	List<Case>		cases			= new ArrayList<>();
	private			BlockNode		defaultBlock	= null;

	public SwitchNode(TokenBuffer tb) {
		super(tb);

		tb.consume(); // Пропускаем "switch"
		tb.consume(Delimiter.LEFT_PAREN);

		// Парсим выражение switch
		this.expression = new ExpressionParser(tb).parse();
		tb.consume(Delimiter.RIGHT_PAREN);
		tb.consume(Delimiter.LEFT_BRACE);

		// Парсим case-блоки
		while (!tb.match(Delimiter.RIGHT_BRACE)) {
			if (tb.match(Keyword.CASE)) {
				parseCase(tb);
			}
			else if (tb.match(Keyword.DEFAULT)) {
				tb.consume(); // Пропускаем "default"
				tb.consume(Delimiter.COLON);
				defaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement());
			}
			else {
				throw new ParseError("Expected 'case' or 'default' in switch statement", sp);
			}
		}
		tb.consume(Delimiter.RIGHT_BRACE);
	}

	private void parseCase(TokenBuffer tb) {
		tb.consume(); // Пропускаем "case"

		// Парсим значение или диапазон
		long from = parseNumber(tb);
		long to = -1;

		if (tb.match(Delimiter.RANGE)) {
			tb.consume(); // Пропускаем ".."
			to = parseNumber(tb);
		}

		tb.consume(Delimiter.COLON);

		cases.add(new Case(from, to, tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement())));
	}

	private long parseNumber(TokenBuffer tb) {
		Token token = tb.consume();
		if(token instanceof TNumber) {
			Number number = (Number)token.getValue();
			if(number instanceof Integer || number instanceof Long) {
				return number.longValue();
			}
		}
		throw new ParseError("Expected numeric value(or range) for 'case' in switch statement", sp);
	}

	public ExpressionNode getExpression() {
		return expression;
	}

	public List<Case> getCases() {
		return cases;
	}

	public BlockNode getDefaultBlock() {
		return defaultBlock;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("switch (");
		sb.append(expression).append(") {\n");

		for (Case c : cases) {
			sb.append("case ").append(c.getFrom());
			if (-1 != c.getTo()) {
				sb.append("..").append(c.getTo());
			}
			sb.append(": ").append(c.getBlock()).append("\n");
		}

		if (null != defaultBlock) {
			sb.append("default: ").append(defaultBlock).append("\n");
		}

		sb.append("}");
		return sb.toString();
	}
}