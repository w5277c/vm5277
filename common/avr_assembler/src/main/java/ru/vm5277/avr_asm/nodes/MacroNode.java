/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.avr_asm.Parser;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.MacroSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.tokens.Token;

public class MacroNode {
	public static void parseDef(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		scope.startMacro(new MacroSymbol(((String)Node.consumeToken(tb, TokenType.ID).getValue()).toLowerCase(), tb.getSP().getLine()));
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
	
	public static void parseCall(TokenBuffer tb, Scope scope, MessageContainer mc, String rtosPath, String basePath, MacroSymbol macro) throws ParseException {
		tb.consume();

		List<Expression> params = new ArrayList<>();
		while(!tb.match(TokenType.EOF) && !tb.match(TokenType.NEWLINE)) {
			params.add(Expression.parse(tb, scope, mc));

			if(tb.match(TokenType.EOF) || tb.match(TokenType.NEWLINE)) break;
			Node.consumeToken(tb, Delimiter.COMMA);
		}

		scope.startMacroImpl(params);
		try {
			for(Token token : macro.getTokens()) {
				token.getSP().setMacroOffset(macro.getName(), tb.getSP().getLine());
			}
			
			Parser parser = new Parser(macro.getTokens(), scope, mc, rtosPath, basePath);
		}
		finally {
			scope.stopMacroImpl();
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
