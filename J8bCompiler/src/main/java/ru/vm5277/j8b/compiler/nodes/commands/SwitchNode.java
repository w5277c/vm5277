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
import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
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

	private	ExpressionNode	expression;
	private	final	List<Case>		cases			= new ArrayList<>();
	private			BlockNode		defaultBlock	= null;

	public SwitchNode(TokenBuffer tb) {
		super(tb);

		consumeToken(tb); // Потребляем "switch"
		// Парсим выражение switch
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
		try {this.expression = new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}
		
		try {consumeToken(tb, Delimiter.LEFT_BRACE);} catch(ParseException e) {markFirstError(e);}
		// Парсим case-блоки
		while (!tb.match(Delimiter.RIGHT_BRACE)) {
			if (tb.match(Keyword.CASE)) {
				parseCase(tb);
			}
			else if (tb.match(Keyword.DEFAULT)) {
				consumeToken(tb); // Потребляем "default"
				try {consumeToken(tb, Delimiter.COLON);} catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().add(this);
				try {defaultBlock = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement());}
				catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
			else {
				markFirstError(tb.error("Expected 'case' or 'default' in switch statement"));
			}
		}
		try {consumeToken(tb, Delimiter.RIGHT_BRACE);}catch(ParseException e) {markFirstError(e);}
	}

	private void parseCase(TokenBuffer tb) {
		consumeToken(tb); // Потребляем "case"

		// Парсим значение или диапазон
		long from = 0;
		long to = -1;

		try {from = parseNumber(tb);} catch(ParseException e) {markFirstError(e);}
		if (tb.match(Delimiter.RANGE)) {
			consumeToken(tb); // Потребляем ".."
			try{to = parseNumber(tb);}catch(ParseException e) {to=0;markFirstError(e);}
		}

		try {consumeToken(tb, Delimiter.COLON);} catch(ParseException e) {markFirstError(e);}
		BlockNode blockNode = null;
		tb.getLoopStack().add(this);
		try {blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb) : new BlockNode(tb, parseStatement());}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);
		cases.add(new Case(from, to, blockNode));
	}

	private long parseNumber(TokenBuffer tb) throws ParseException {
		Token token = consumeToken(tb);
		if(token instanceof TNumber) {
			Number number = (Number)token.getValue();
			if(number instanceof Integer || number instanceof Long) {
				return number.longValue();
			}
		}
		throw tb.error("Expected numeric value(or range) for 'case' in switch statement");
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