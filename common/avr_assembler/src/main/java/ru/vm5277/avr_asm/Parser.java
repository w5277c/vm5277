/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.util.ArrayList;
import ru.vm5277.avr_asm.nodes.DataNode;
import ru.vm5277.avr_asm.nodes.*;
import java.util.List;
import java.util.Map;
import ru.vm5277.avr_asm.scope.MacroDefSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.MessageOwner;
import ru.vm5277.avr_asm.tokens.Token;

public class Parser {
	private	final	TokenBuffer				tb;
	private	final	Scope					scope;
	private	final	MessageContainer		mc;
	private	final	Map<String, SourceType>	sourcePaths;
	private	final	List<MnemNode>			secondPassNodes	= new ArrayList<>();
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, Map<String, SourceType> sourcePaths, int tabSize) throws CriticalParseException {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.sourcePaths = sourcePaths;
		
		scope.setTabSize(tabSize);
		
		mc.setOwner(MessageOwner.PARSER);
		parse();
	}
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, Map<String, SourceType> sourcePaths) throws CriticalParseException {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.sourcePaths = sourcePaths;
		
		mc.setOwner(MessageOwner.PARSER);
		parse();
	}
	
	void parse() throws CriticalParseException {
		while(!tb.match(TokenType.EOF)) {
			try {
				if(scope.isMacroDef()) {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(Keyword.ENDM.getName().equals(kd) || Keyword.ENDMACRO.getName().equals(kd)) {EndMacroNode.parse(tb, scope, mc);continue;}
					}
					scope.getMacroDef().addToken(tb.consume());
					continue;
				}
			
				if(tb.match(TokenType.NEWLINE)) {
					tb.consume();
					continue;
				}

				if(scope.getIncludeSymbol().isBlockSkip()) {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(Keyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(Keyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(Keyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						if(Keyword.ELSE.getName().equals(kd)) {ElseNode.parse(tb, scope, mc); continue;}						
						//TODO остальные директивы с условиями
					}
					tb.skipLine(); continue;
				}
				else {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(!scope.isMacroCall()) {
							if(Keyword.INCLUDE.getName().equals(kd))	{IncludeNode.parse(tb, scope, mc, sourcePaths); continue;}
							if(Keyword.ORG.getName().equals(kd)) {OrgNode.parse(tb, scope, mc); continue;}
							if(Keyword.DEVICE.getName().equals(kd)) {DeviceNode.parse(tb, scope, mc); continue;}
							if(Keyword.EQU.getName().equals(kd)) {EquNode.parse(tb, scope, mc); continue;}
							if(Keyword.MACRO.getName().equals(kd)) {MacroNode.parseDef(tb, scope, mc); continue;}
						}
						if(Keyword.DEF.getName().equals(kd)) {DefNode.parse(tb, scope, mc); continue;}
						if(Keyword.SET.getName().equals(kd))	{SetNode.parse(tb, scope, mc); continue;}
						if(Keyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(Keyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(Keyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						if(Keyword.ELSE.getName().equals(kd)) {ElseNode.parse(tb, scope, mc); continue;}
						if(Keyword.MESSAGE.getName().equals(kd)) {MessageNode.parse(tb, scope, mc); continue;}
						if(Keyword.DB.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 1); continue;}
						if(Keyword.DW.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 2); continue;}
						if(Keyword.DD.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 4); continue;}
						if(Keyword.DQ.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 8); continue;}

						mc.add(new ErrorMessage("Unexpected directive: " + kd + (scope.isMacroDef() ? " in macro" : ""), tb.getSP()));
						tb.skipLine();
						continue;					
					}
					if(tb.match(TokenType.MNEMONIC)) {
						try {secondPassNodes.add(new MnemNode(tb, scope, mc));}
						catch(ParseException e) {
							mc.add(e.getErrorMessage());
						}
						continue;
					}
					if(tb.match(TokenType.LABEL)) {
						LabelNode.parse(tb, scope, mc); continue;
					}
					if(tb.match(TokenType.ID)) {
						String str = ((String)tb.current().getValue()).toLowerCase();
						MacroDefSymbol macro = scope.resolveMacro(str);
						if(null != macro) {
							MacroNode.parseCall(tb, scope, mc, sourcePaths, macro);
							continue;
						}
					}
				}
				if(tb.match(TokenType.NEWLINE)) {
					tb.consume();
					continue;
				}
				mc.add(new ErrorMessage("Unexpected statement token: " + tb.consume().getType(), tb.getSP()));
				tb.skipLine();
			}
			catch(ParseException e) {
				mc.add(e.getErrorMessage());
				tb.skipLine();
			}
		}
	}
	
	public List<MnemNode> getSecondPassNodes() {
		return secondPassNodes;
	}
}
