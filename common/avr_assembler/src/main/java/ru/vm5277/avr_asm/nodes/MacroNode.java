/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import ru.vm5277.avr_asm.Parser;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.MacroDefSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.Delimiter;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.avr_asm.tokens.Token;

public class MacroNode {
	public static void parseDef(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		try{
			String name = ((String)Node.consumeToken(tb, TokenType.ID).getValue()).toLowerCase();
			scope.startMacro(new MacroDefSymbol(name, tb.getSP().getLine()), tb.getSP());

			scope.list(".MACRO " + name);
		}
		catch(ParseException e) {
			mc.add(e.getErrorMessage());
			tb.skipLine();
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
	
	public static void parseCall(TokenBuffer tb, Scope scope, MessageContainer mc, Map<String, SourceType> sourcePaths, MacroDefSymbol macro)
																												throws ParseException, CriticalParseException {
		tb.consume();

		List<Expression> params = new ArrayList<>();
		while(!tb.match(TokenType.EOF) && !tb.match(TokenType.NEWLINE)) {
			params.add(Expression.parse(tb, scope, mc));

			if(tb.match(TokenType.EOF) || tb.match(TokenType.NEWLINE)) break;
			Node.consumeToken(tb, Delimiter.COMMA);
		}

		if(scope.isListMacEnabled()) scope.list("# MACRO IMPL " + macro.getName() + " BEGIN " + params);
		scope.startMacroImpl(macro.getName(), params);
		try {
			for(Token token : macro.getTokens()) {
				token.getSP().setMacroOffset(macro.getName(), tb.getSP().getLine());
			}
			Parser parser = new Parser(macro.getTokens(), scope, mc, sourcePaths);
			for(MnemNode node : parser.getSecondPassNodes()) {
				try {node._secondPass();} catch(Exception e) {}
			}
		}
		finally {
			if(scope.isListMacEnabled()) scope.list("# MACRO IMPL " + macro.getName() + " END");
			scope.stopMacroImpl();
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
}
