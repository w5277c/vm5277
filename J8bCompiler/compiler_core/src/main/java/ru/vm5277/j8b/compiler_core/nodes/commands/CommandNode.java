/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
05.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.commands;

import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.ReturnStatus;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.AstNode;
import ru.vm5277.j8b.compiler_core.nodes.BlockNode;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler_core.semantic.BlockScope;
import ru.vm5277.j8b.compiler_core.semantic.LabelSymbol;
import ru.vm5277.j8b.compiler_core.semantic.MethodScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;
import ru.vm5277.j8b.compiler_core.tokens.TNumber;
import ru.vm5277.j8b.compiler_core.tokens.Token;

public abstract class CommandNode extends AstNode {
	public static class AstCase {
		private	final	long		from;
		private	final	Long		to;
		private	final	BlockNode	block;
		private			BlockScope	scope;
		
		public AstCase(long from, Long to, BlockNode block) {
			this.from = from;
			this.to = to;
			this.block = block;
		}
		
		public long getFrom() {
			return from;
		}
		
		public Long getTo() {
			return to;
		}
		
		public BlockNode getBlock() {
			return block;
		}
		
		public BlockScope getScope() {
			return scope;
		}
		public void setScope(BlockScope scope) {
			this.scope = scope;
		}
	}

	public CommandNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
	}
	
	protected boolean isLoopOrSwitch(CommandNode node) {
		return null != node && (node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode || node instanceof SwitchNode);
	}
	
	
	protected boolean isLabelInCurrentMethod(LabelSymbol symbol, Scope scope) {
		// Ищем метод в текущей цепочке областей видимости
		Scope current = scope;
		while (null != current) {
			if (current instanceof MethodScope) break;
			current = current.getParent();
		}

		// Ищем метод, содержащий метку
		Scope labelScope = symbol.getScope();
		while (null != labelScope) {
			if (labelScope instanceof MethodScope) {
				return labelScope == current;
			}
			labelScope = labelScope.getParent();
		}

		return false;
	}

	// Анализирует наличие return в команде
	public ReturnStatus getReturnStatus() {
		return ReturnStatus.NEVER;
	}
	
	protected AstCase parseCase(TokenBuffer tb, MessageContainer mc) {
		consumeToken(tb); // Потребляем "case"

		// Парсим значение или диапазон
		long from = 0;
		Long to = null;

		try {from = parseNumber(tb);} catch(ParseException e) {markFirstError(e);}
		if (tb.match(Delimiter.RANGE)) {
			consumeToken(tb); // Потребляем ".."
			try{to = parseNumber(tb);}catch(ParseException e) {to=0l;markFirstError(e);}
		}

		try {consumeToken(tb, Delimiter.COLON);} catch(ParseException e) {markFirstError(e);}
		BlockNode blockNode = null;
		tb.getLoopStack().add(this);
		try {blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);
		return new AstCase(from, to, blockNode);
	}

	private long parseNumber(TokenBuffer tb) throws ParseException {
		Token token = consumeToken(tb);
		if(token instanceof TNumber) {
			Number number = (Number)token.getValue();
			if(number instanceof Integer || number instanceof Long) {
				return number.longValue();
			}
		}
		throw parserError("Expected numeric value(or range) for 'case' in switch statement");
	}
}
