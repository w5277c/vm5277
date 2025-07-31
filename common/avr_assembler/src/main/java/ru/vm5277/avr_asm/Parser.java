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
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.avr_asm.tokens.Token;
import ru.vm5277.common.SourcePosition;

public class Parser {
	private	final	TokenBuffer				tb;
	private	final	Scope					scope;
	private	final	MessageContainer		mc;
	private	final	Map<Path, SourceType>	sourcePaths;
	private	final	List<Node>				secondPassNodes	= new ArrayList<>();
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, Map<Path, SourceType> sourcePaths, int tabSize) throws CriticalParseException {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.sourcePaths = sourcePaths;
		
		scope.setTabSize(tabSize);
		
		parse();
	}
	
	public Parser(List<Token> tokens, Scope scope, MessageContainer mc, Map<Path, SourceType> sourcePaths) throws CriticalParseException {
		this.tb = new TokenBuffer(tokens.iterator());
		this.scope = scope;
		this.mc = mc;
		this.sourcePaths = sourcePaths;
		
		parse();
	}
	
	void parse() throws CriticalParseException {
		while(!tb.match(TokenType.EOF)) {
			try {
				if(tb.match(TokenType.DIRECTIVE) && Keyword.EXIT == ((Keyword)tb.current().getValue())) {
					scope.list(".EXIT");
					break;
				}

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
						if(Keyword.IFNDEF.getName().equals(kd)) {IfNDefNode.parse(tb, scope, mc); continue;}
						if(Keyword.ELSE.getName().equals(kd)) {ElseNode.parse(tb, scope, mc); continue;}
						if(Keyword.ELSEIF.getName().equals(kd) || Keyword.ELIF.getName().equals(kd)) {ElseIfNode.parse(tb, scope, mc); continue;}
						//TODO остальные директивы с условиями
					}
					tb.skipLine(); continue;
				}
				else {
					if(tb.match(TokenType.DIRECTIVE)) {
						String kd = ((Keyword)tb.consume().getValue()).getName();
						if(!scope.isMacroDeploy()) {
							if(Keyword.INCLUDE.getName().equals(kd))	{
								Parser parser = IncludeNode.parse(tb, scope, mc, sourcePaths);
								if(null != parser) secondPassNodes.addAll(parser.getSecondPassNodes());
								continue;
							}
							if(Keyword.ORG.getName().equals(kd)) {OrgNode.parse(tb, scope, mc); continue;}
							if(Keyword.DEVICE.getName().equals(kd)) {DeviceNode.parse(tb, scope, mc); continue;}
							if(Keyword.EQU.getName().equals(kd)) {EquNode.parse(tb, scope, mc); continue;}
							if(Keyword.MACRO.getName().equals(kd)) {MacroNode.parseDef(tb, scope, mc); continue;}
						}
						if(Keyword.DEF.getName().equals(kd)) {DefNode.parse(tb, scope, mc); continue;}
						if(Keyword.UNDEF.getName().equals(kd)) {UndefNode.parse(tb, scope, mc); continue;}
						if(Keyword.SET.getName().equals(kd))	{SetNode.parse(tb, scope, mc); continue;}
						if(Keyword.IF.getName().equals(kd)) {IfNode.parse(tb, scope, mc); continue;}
						if(Keyword.ENDIF.getName().equals(kd)) {EndIfNode.parse(tb, scope, mc); continue;}
						if(Keyword.IFDEF.getName().equals(kd)) {IfDefNode.parse(tb, scope, mc); continue;}
						if(Keyword.IFNDEF.getName().equals(kd)) {IfNDefNode.parse(tb, scope, mc); continue;}
						if(Keyword.ELSE.getName().equals(kd)) {ElseNode.parse(tb, scope, mc); continue;}
						if(Keyword.ELSEIF.getName().equals(kd) || Keyword.ELIF.getName().equals(kd)) {ElseIfNode.parse(tb, scope, mc); continue;}
						if(Keyword.MESSAGE.getName().equals(kd)) {MessageNode.parse(tb, scope, mc); continue;}
						if(Keyword.WARNING.getName().equals(kd)) {WarningNode.parse(tb, scope, mc); continue;}
						if(Keyword.ERROR.getName().equals(kd)) {ErrorNode.parse(tb, scope, mc); continue;}
						if(Keyword.DB.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 1); continue;}
						if(Keyword.DW.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 2); continue;}
						if(Keyword.DD.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 4); continue;}
						if(Keyword.DQ.getName().equals(kd)) {DataNode.parse(tb, scope, mc, 8); continue;}
						if(Keyword.LIST.getName().equals(kd)) {scope.setListEnabled(true); continue;}
						if(Keyword.NOLIST.getName().equals(kd)) {scope.setListEnabled(false); continue;}
						if(Keyword.OVERLAP.getName().equals(kd)) {scope.setOverlapAllowed(true); continue;}
						if(Keyword.NOOVERLAP.getName().equals(kd)) {scope.setOverlapAllowed(false); continue;}
						if(Keyword.LISTMAC.getName().equals(kd)) {scope.setListMacEnabled(true); continue;}
						if(Keyword.NOLISTMAC.getName().equals(kd)) {scope.setListMacEnabled(false); continue;}
						
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
					if(tb.match(TokenType.ID)) {
						SourcePosition sp = tb.getSP();
						String str = ((String)tb.current().getValue()).toLowerCase();
						MacroDefSymbol macro = scope.resolveMacro(str);
						if(null != macro) {
							MacroNode node = MacroNode.parseCall(tb, scope, mc, sourcePaths, macro);
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
