/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import ru.vm5277.avr_asm.nodes.DataNode;
import ru.vm5277.avr_asm.nodes.*;
import java.util.List;
import ru.vm5277.avr_asm.scope.MacroSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.AsmKeyword;
import ru.vm5277.common.Keyword;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.MessageOwner;
import ru.vm5277.common.tokens.Token;

public class Parser {
	private	final	TokenBuffer				tb;
	private	final	Scope					scope;
	private	final	MessageContainer		mc;
	private	final	String					rtosPath;
	private	final	String					basePath;
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, String rtosPath, String basePath, int tabSize) {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.rtosPath = rtosPath;
		this.basePath = basePath;
		
		scope.setTabSize(tabSize);
		
		mc.setOwner(MessageOwner.PARSER);
		parse();
	}
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, String rtosPath, String basePath) {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.rtosPath = rtosPath;
		this.basePath = basePath;
		
		mc.setOwner(MessageOwner.PARSER);
		parse();
	}
	
	void parse() {
		while(!tb.match(TokenType.EOF)) {
			try {
				if(scope.isMacro()) {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(AsmKeyword.ENDM.getName().equals(kd) || AsmKeyword.ENDMACRO.getName().equals(kd)) {EndMacroNode.parse(tb, scope, mc);continue;}
					}
					scope.getCurrentMacro().addToken(tb.consume());
					continue;
				}
			
				if(tb.match(TokenType.NEWLINE)) {
					tb.consume();
					continue;
				}

				if(scope.isBlockSkip()) {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(!scope.isMacroMode()) {
							if(AsmKeyword.MACRO.getName().equals(kd)) {MacroNode.parseDef(tb, scope, mc); continue;}
						}
						if(AsmKeyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						//TODO остальные директивы с условиями
					}
				}
				else {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(!scope.isMacroMode()) {
							if(AsmKeyword.INCLUDE.getName().equals(kd))	{IncludeNode.parse(tb, scope, mc, rtosPath, basePath); continue;}
							if(AsmKeyword.ORG.getName().equals(kd)) {OrgNode.parse(tb, scope, mc); continue;}
							if(AsmKeyword.DEVICE.getName().equals(kd)) {DeviceNode.parse(tb, scope, mc); continue;}
							if(AsmKeyword.MNEMONICS.getName().equals(kd)) {MnemonicsNode.parse(tb, scope, mc); continue;}
							if(AsmKeyword.EQU.getName().equals(kd)) {EquNode.parse(tb, scope, mc); continue;}
							if(AsmKeyword.MACRO.getName().equals(kd)) {MacroNode.parseDef(tb, scope, mc); continue;}
						}
						if(AsmKeyword.DEF.getName().equals(kd)) {DefNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.SET.getName().equals(kd))	{SetNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						if(AsmKeyword.DB.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 1); continue;}
						if(AsmKeyword.DW.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 2); continue;}
						if(AsmKeyword.DD.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 4); continue;}
						if(AsmKeyword.DQ.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 8); continue;}

						mc.add(new ErrorMessage("Unexpected directive: " + kd + (scope.isMacro() ? " in macro" : ""), tb.getSP()));
						tb.skipLine();
						continue;					
					}
					if(tb.match(TokenType.MNEMONIC)) {
						try {scope.addSecondPassNode(new MnemNode(tb, scope, mc));}
						catch(ParseException e) {
							mc.add(e.getErrorMessage());
						}
						continue;
					}
					if(tb.match(TokenType.LABEL)) {
						LabelNode.parse(tb, scope, mc); continue;
					}
					if(tb.match(TokenType.ID)) {
						MacroSymbol macro = scope.resolveMacro(((String)tb.current().getValue()).toLowerCase());
						if(null != macro) {
							MacroNode.parseCall(tb, scope, mc, rtosPath, basePath, macro);
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
}
