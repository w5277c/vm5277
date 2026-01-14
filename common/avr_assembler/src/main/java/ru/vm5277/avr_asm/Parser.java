/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.vm5277.avr_asm;

import ru.vm5277.common.lexer.ASMKeyword;
import ru.vm5277.common.SourceType;
import java.nio.file.Path;
import java.util.ArrayList;
import ru.vm5277.avr_asm.nodes.DataNode;
import ru.vm5277.avr_asm.nodes.*;
import java.util.List;
import java.util.Map;
import ru.vm5277.avr_asm.scope.MacroDefSymbol;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;

public class Parser {
	private	final	TokenBuffer				tb;
	private	final	Scope					scope;
	private	final	MessageContainer		mc;
	private	final	Map<Path, SourceType>	sourcePaths;
	private	final	List<Node>				secondPassNodes	= new ArrayList<>();
	private	final	int						tabSize;
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, Map<Path, SourceType> sourcePaths, int tabSize, List<String> includes)
																																throws CriticalParseException {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.sourcePaths = sourcePaths;
		this.tabSize = tabSize;
		
		parse(includes);
	}
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, Map<Path, SourceType> sourcePaths, int tabSize) throws CriticalParseException {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.sourcePaths = sourcePaths;
		this.tabSize = tabSize;
		
		parse(null);
	}
	
	void parse(List<String> includes) throws CriticalParseException {
		try {
			if(null!=includes) {
				for(String inclideName : includes) {
					Parser parser = IncludeNode.parse(tb, scope, mc, sourcePaths, inclideName, tabSize);
					if(null!=parser) {
						secondPassNodes.addAll(parser.getSecondPassNodes());
					}
				}
			}
		}
		catch(CompileException e) {
			mc.add(e.getErrorMessage());
		}

		while(!tb.match(TokenType.EOF)) {
			try {
				if(tb.match(TokenType.DIRECTIVE) && ASMKeyword.EXIT == tb.current().getValue()) {
					scope.list(".EXIT");
					break;
				}

				if(scope.isMacroDef()) {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(ASMKeyword.ENDM.getName().equals(kd) || ASMKeyword.ENDMACRO.getName().equals(kd)) {EndMacroNode.parse(tb, scope, mc);continue;}
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
						if(ASMKeyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.IFNDEF.getName().equals(kd)) {IfNDefNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ELSE.getName().equals(kd)) {ElseNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ELSEIF.getName().equals(kd) || ASMKeyword.ELIF.getName().equals(kd)) {ElseIfNode.parse(tb, scope, mc); continue;}
						//TODO остальные директивы с условиями
					}
					tb.skipLine(); continue;
				}
				else {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(!scope.isMacroDeploy()) {
							if(ASMKeyword.INCLUDE.getName().equals(kd))	{
								Parser parser = IncludeNode.parse(tb, scope, mc, sourcePaths, null, tabSize);
								if(null != parser) secondPassNodes.addAll(parser.getSecondPassNodes());
								continue;
							}
							if(ASMKeyword.ORG.getName().equals(kd)) {OrgNode.parse(tb, scope, mc); continue;}
							if(ASMKeyword.DEVICE.getName().equals(kd)) {DeviceNode.parse(tb, scope, mc); continue;}
							if(ASMKeyword.EQU.getName().equals(kd)) {EquNode.parse(tb, scope, mc); continue;}
							if(ASMKeyword.MACRO.getName().equals(kd)) {MacroNode.parseDef(tb, scope, mc); continue;}
						}
						if(ASMKeyword.DEF.getName().equals(kd)) {DefNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.UNDEF.getName().equals(kd)) {UndefNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.SET.getName().equals(kd))	{SetNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.IFNDEF.getName().equals(kd)) {IfNDefNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ELSE.getName().equals(kd)) {ElseNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ELSEIF.getName().equals(kd) || ASMKeyword.ELIF.getName().equals(kd)) {ElseIfNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.MESSAGE.getName().equals(kd)) {MessageNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.WARNING.getName().equals(kd)) {WarningNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.ERROR.getName().equals(kd)) {ErrorNode.parse(tb, scope, mc); continue;}
						if(ASMKeyword.DB.getName().equals(kd)) {secondPassNodes.add(new DataNode(tb, scope, mc, 1)); continue;}
						if(ASMKeyword.DW.getName().equals(kd)) {secondPassNodes.add(new DataNode(tb, scope, mc, 2)); continue;}
						if(ASMKeyword.DD.getName().equals(kd)) {secondPassNodes.add(new DataNode(tb, scope, mc, 4)); continue;}
						if(ASMKeyword.DQ.getName().equals(kd)) {secondPassNodes.add(new DataNode(tb, scope, mc, 8)); continue;}
						if(ASMKeyword.LIST.getName().equals(kd)) {scope.setListEnabled(true); continue;}
						if(ASMKeyword.NOLIST.getName().equals(kd)) {scope.setListEnabled(false); continue;}
						if(ASMKeyword.OVERLAP.getName().equals(kd)) {scope.setOverlapAllowed(true); continue;}
						if(ASMKeyword.NOOVERLAP.getName().equals(kd)) {scope.setOverlapAllowed(false); continue;}
						if(ASMKeyword.LISTMAC.getName().equals(kd)) {scope.setListMacEnabled(true); continue;}
						if(ASMKeyword.NOLISTMAC.getName().equals(kd)) {scope.setListMacEnabled(false); continue;}
						
						mc.add(new ErrorMessage("Unexpected directive: " + kd + (scope.isMacroDef() ? " in macro" : ""), tb.getSP()));
						tb.skipLine();
						continue;					
					}
					if(tb.match(TokenType.MNEMONIC)) {
						try {secondPassNodes.add(new MnemNode(tb, scope, mc));}
						catch(CompileException e) {
							mc.add(e.getErrorMessage());
						}
						continue;
					}
					if(tb.match(TokenType.LABEL)) {
						LabelNode.parse(tb, scope, mc); continue;
					}
					if(tb.match(TokenType.IDENTIFIER)) {
						SourcePosition sp = tb.getSP();
						String str = ((String)tb.current().getValue()).toLowerCase();
						MacroDefSymbol macro = scope.resolveMacro(str);
						if(null != macro) {
							MacroNode node = MacroNode.parseCall(tb, scope, mc, sourcePaths, macro, tabSize);
							secondPassNodes.add(node);
							continue;
						}
						else {
							mc.add(new ErrorMessage("Unable to resolve macro: '" + str + "'", sp));
							tb.skipLine();
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
			catch(CompileException e) {
				mc.add(e.getErrorMessage());
				tb.skipLine();
			}
		}
	}
	
	public List<Node> getSecondPassNodes() {
		return secondPassNodes;
	}
}
