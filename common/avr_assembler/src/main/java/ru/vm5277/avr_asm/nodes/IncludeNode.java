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
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.common.SourceType;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import ru.vm5277.avr_asm.Assembler;
import ru.vm5277.avr_asm.Lexer;
import ru.vm5277.avr_asm.Parser;
import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.TokenType;
import ru.vm5277.common.exceptions.CriticalParseException;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;

public class IncludeNode {
	public static Parser parse(TokenBuffer tb, Scope scope, MessageContainer mc, Map<Path, SourceType> sourcePaths, String includeName)
																											throws CompileException, CriticalParseException {
		String importPath = (null==includeName ? (String)Node.consumeToken(tb, TokenType.STRING).getValue() : includeName);
		Parser parser = null;

		SourcePosition sp = (null==includeName ? tb.getSP() : null);
		
		Path sourcePath = null;
		for(Path path : sourcePaths.keySet()) {
			Path path2 = path.resolve(importPath).normalize();
			if (path2.toFile().exists()) {
				sourcePath = path2;
				break;
			}
		}
		if(null==sourcePath) {
			throw new CompileException("Import file not found: " + importPath, sp);
		}

		if(!scope.addImport(sourcePath.toString())) {
			if(Assembler.STRICT_STRONG == Scope.getStrincLevel()) {
				mc.add(new WarningMessage("File '" + importPath + "' already imported", sp));
			}
		}
		else {
			scope.list(".INCLUDE " + sourcePath.toString());
			
			try {
				Lexer lexer = new Lexer(sourcePath.toFile(), scope, mc);
				Map<Path, SourceType> innerSourcePaths = new HashMap<>(sourcePaths);
				innerSourcePaths.put(sourcePath.getParent(), SourceType.LIB);
				parser = new Parser(lexer.getTokens(), scope, mc, innerSourcePaths);
				
			}
			catch(IOException e) {
				mc.add(new ErrorMessage(e.getMessage(), sp));
			}
			finally {
				try {scope.leaveImport(sp);}
				catch(CompileException e) {mc.add(e.getErrorMessage());}
				catch(CriticalParseException e) {
					mc.add(e.getErrorMessage()); 
					Node.consumeToken(tb, TokenType.NEWLINE);
					throw e;
				}
			}
		}
		Node.consumeToken(tb, TokenType.NEWLINE);
		
		return parser;
	}
}
