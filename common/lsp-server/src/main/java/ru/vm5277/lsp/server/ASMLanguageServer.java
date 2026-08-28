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

import ru.vm5277.lsp.server.tokens_info.LSPNodeInfo;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
	private	final	static	PlatformType				platformType	= PlatformType.AVR;
	private					Lexer						lexer;
	private					AsmInstrSimpleReader		insrReader;
	private					DefReader					defReader		= null;
	private			static	Map<String,LSPNodeInfo>	keywords		= null;
	private final			List<String>				libraries		= new ArrayList<>();
	
	static {
		if(null==keywords) {
			keywords = new HashMap<>();
			for(Keyword kw : Keyword.getItems()) {
				if(LexerType.ALL==kw.getLexerType() || LexerType.ASM==kw.getLexerType()) {
					keywords.put(kw.getName().toLowerCase(), new LSPNodeInfo(kw.getName(), kw.getTokenType(), "keyword", 100));
				}
			}
		}
	}
	
	public ASMLanguageServer(final PlatformType platformType, String mcuName) {
		try {
			Path toolkitPath = FSUtils.getToolkitPath();
			Path defsPath = toolkitPath.resolve("defs").resolve(platformType.name().toLowerCase()).normalize();
			Path rtosPath = toolkitPath.resolve("rtos").resolve(platformType.name().toLowerCase()).normalize();
			Path devicesPath = rtosPath.resolve("devices").normalize();
			final Platform platform = new Platform(rtosPath, platformType);
			
			insrReader = new AsmInstrSimpleReader(defsPath);
			defReader = new DefReader();
			if(null!=defsPath) {
				defReader.parse(defsPath.resolve("common.asm"));
				if(null!=mcuName) defReader.parse(defsPath.resolve(mcuName + ".asm"));
			}
			if(null!=devicesPath) {
				defReader.parse(devicesPath.resolve("_common.def"));
				defReader.parse(devicesPath.resolve("_features.def"));
				if(null!=mcuName) defReader.parse(devicesPath.resolve(mcuName + ".def"));
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
			
			loadLibraries(rtosPath);
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void loadLibraries(Path devicesPath) {
		try {
			List<Path> files = new ArrayList<>();
			collectAsmFiles(devicesPath.toFile(), files);

			for(Path path : files) {
				String name = devicesPath.relativize(path).toString().replace("\\", "/");
				libraries.add(name.substring(0, name.length()));
			}
			Collections.sort(libraries);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void collectAsmFiles(File dir, List<Path> result) {
		File[] files = dir.listFiles();
		if(null==files) return;

		for(File file : files) {
			if(file.isDirectory()) {
				collectAsmFiles(file, result);
			}
			else if(file.getName().endsWith(".asm")) {
				result.add(file.toPath());
			}
		}
	}

	public List<String> getImportSuggestions(String prefix) {
		List<String> result = new ArrayList<>();
		for(String name : libraries) {
			if(name.startsWith(prefix)) {
				result.add(name);
			}
		}
		return result;
	}

	public List<LSPNodeInfo> getCompletionSuggestions(String prefix) {
		String prefixLc = (null==prefix ? null : prefix.toLowerCase());
		
		List<LSPNodeInfo> result = new ArrayList<>();
		for(Entry<String, LSPNodeInfo> entry : keywords.entrySet()) {
			if(null==prefixLc || prefixLc.isEmpty() || entry.getKey().startsWith(prefixLc)) {
				result.add(entry.getValue());
			}
		}
		for(String macro : defReader.getMacros()) {
			if(null==prefixLc || macro.toLowerCase().startsWith(prefixLc)) {
				result.add(new LSPNodeInfo(macro.toUpperCase(), TokenType.MACRO, "keyword", 4));
			}
		}			
		for(String register : defReader.getNumericRegisters()) {
			if(null==prefixLc || register.toLowerCase().startsWith(prefixLc)) {
				result.add(new LSPNodeInfo(register.toUpperCase(), TokenType.REGISTER, "keyword", 3));
			}
		}
		for(String register : defReader.getRegisters()) {
			if(null==prefixLc || register.toLowerCase().startsWith(prefixLc)) {
				result.add(new LSPNodeInfo(register.toUpperCase(), TokenType.REGISTER, "keyword", 2));
			}
		}
		for(String instr : insrReader.getInstrs()) {
			if(null==prefixLc || instr.toLowerCase().startsWith(prefixLc)) {
				result.add(new LSPNodeInfo(instr.toUpperCase(), TokenType.MNEMONIC, "keyword", 1));
			}
		}
		
		Collections.sort(result, new Comparator<LSPNodeInfo>() {
			@Override
			public int compare(LSPNodeInfo o1, LSPNodeInfo o2) {
				if(o1.getPriority()!=o2.getPriority()) return Integer.compare(o1.getPriority(), o2.getPriority());
				return o1.getName().compareTo(o2.getName());
			}
		});
		return result;
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
