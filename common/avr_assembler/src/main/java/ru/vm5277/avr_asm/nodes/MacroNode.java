/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import java.nio.file.Path;
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
import ru.vm5277.avr_asm.scope.MacroCallSymbol;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.avr_asm.tokens.Token;
import ru.vm5277.common.SourcePosition;

public class MacroNode extends Node {
	private final	MacroCallSymbol	callSymbol;
	
	public MacroNode(MacroCallSymbol callSymbol) {
		this.callSymbol = callSymbol;
	}
	
	public MacroCallSymbol getCallSymbol() {
		return callSymbol;
	}
	
	public static void parseDef(TokenBuffer tb, Scope scope, MessageContainer mc) throws ParseException {
		try{
			SourcePosition sp = tb.getSP();
			String name = ((String)Node.consumeToken(tb, TokenType.ID).getValue()).toLowerCase();
			scope.beginMacro(new MacroDefSymbol(name, sp), sp);

			scope.list(".MACRO " + name);
		}
		catch(ParseException e) {
			mc.add(e.getErrorMessage());
			tb.skipLine();
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
	}
	
	public static MacroNode parseCall(TokenBuffer tb, Scope scope, MessageContainer mc, Map<Path, SourceType> sourcePaths, MacroDefSymbol macro)
																												throws ParseException, CriticalParseException {
		SourcePosition callSP = tb.getSP();
		MacroCallSymbol symbol = null;
		tb.consume();
		
		List<Expression> params = new ArrayList<>();
		while(!tb.match(TokenType.EOF) && !tb.match(TokenType.NEWLINE)) {
			params.add(Expression.parse(tb, scope, mc));

			if(tb.match(TokenType.EOF) || tb.match(TokenType.NEWLINE)) break;
			Node.consumeToken(tb, Delimiter.COMMA);
		}

		if(scope.isListMacEnabled()) scope.list("# MACRO IMPL " + macro.getName() + " BEGIN " + params);
		symbol = new MacroCallSymbol(macro.getName(), params);
		scope.beginMacroDeploy(symbol);
		try {
			for(Token token : macro.getTokens()) {
				token.getSP().setMacroOffset(callSP, macro.getName());
			}
			Parser parser = new Parser(macro.getTokens(), scope, mc, sourcePaths);
			symbol.setSecondPartNodes(parser.getSecondPassNodes());
// TODO метка может быть после тела макроса, а значит secondPass должен отработать после первого прохода
		}
		finally {
			if(scope.isListMacEnabled()) scope.list("# MACRO IMPL " + macro.getName() + " END");
			scope.endMacroDeploy();
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
		return new MacroNode(symbol);
	}
}
