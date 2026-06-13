/*
 * Copyright 2026 konstantin@5277.ru
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

package ru.vm5277.lsp.server;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.AsmInstrSimpleReader;
import ru.vm5277.common.DefReader;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Platform;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.lexer.ExternalTokenProvider;
import ru.vm5277.common.lexer.Keyword;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.tokens.Token;

public class ASMLanguageServer extends LanguageServer {
	private	final	static	PlatformType			platformType	= PlatformType.AVR;
	private					Lexer					lexer;
	private					AsmInstrSimpleReader	insrReader;
	
	public ASMLanguageServer(final PlatformType platformType, String mcuName) {
		try {
			Path toolkitPath = FSUtils.getToolkitPath();
			Path defsPath = toolkitPath.resolve("defs").resolve(platformType.name().toLowerCase()).normalize();
			Path rtosPath = toolkitPath.resolve("rtos").resolve(platformType.name().toLowerCase()).normalize();
			Path devicesPath = rtosPath.resolve("devices").normalize();
			final Platform platform = new Platform(rtosPath, platformType);
			
			insrReader = new AsmInstrSimpleReader(defsPath);
			final DefReader defReader = new DefReader();
			if(null!=defsPath) {
				defReader.parse(defsPath.resolve("common.asm"));
				defReader.parse(defsPath.resolve(mcuName + ".asm"));
			}
			if(null!=devicesPath) {
				defReader.parse(devicesPath.resolve("_common.def"));
				defReader.parse(devicesPath.resolve("_features.def"));
				defReader.parse(devicesPath.resolve(mcuName + ".def"));
			}
			
			ExternalTokenProvider tokenProvider = new ExternalTokenProvider() {
				@Override
				public Token getExternalToken(SourceBuffer sb, SourcePosition sp, String str) {
					if(defReader.getMacros().contains(str)) {
						return new Token(sb, sp, TokenType.MACRO, str.toLowerCase()); //TODO - временно, нужно убрать после реализации парсинга в LSP 
					}
					if(platform.getRegisters().contains(str) || null!=defReader.getRegister(str)) {
						return new Token(sb, sp, TokenType.REGISTER, str.toLowerCase());
					}
					else if(insrReader.getInstrs().contains(str.toLowerCase())) {
						return new Token(sb, sp, TokenType.MNEMONIC, str.toLowerCase());
					}
					return null;
				}
			};
			
			lexer = new Lexer(LexerType.ASM, tokenProvider);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public LSPToken readNextToken(SourceBuffer sb) {
		Token token = lexer.parseToken(sb);
		if(null==token || TokenType.EOF==token.getType()) {
			return null;
		}
		
		if(TokenType.LABEL==token.getType()) {
			labels.add(token.getRaw());
		}
		
		return new LSPToken(token);
	}

	@Override
	public List<Token> tokenize(SourceBuffer sb) {
		List<Token> result = new ArrayList<>();
		while(sb.available()) {
			result.add(lexer.parseToken(sb));
		}
		return result;
	}
	
	public Set<String> getInstructions() {
		return insrReader.getInstrs();
	}
	
	public static List<String> getKeywords() {
		List<String> result = new ArrayList<>();
		for(Keyword keyword : Keyword.getItems()) {
			if(LexerType.ALL==keyword.getLexerType() || LexerType.ASM==keyword.getLexerType()) {
				result.add(keyword.getName().toUpperCase());
			}
		}
		return result;
	}
}
