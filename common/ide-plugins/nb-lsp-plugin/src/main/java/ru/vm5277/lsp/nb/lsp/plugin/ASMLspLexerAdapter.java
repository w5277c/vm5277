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

package ru.vm5277.lsp.nb.lsp.plugin;

import java.io.IOException;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.api.lexer.Token;
import java.util.logging.Logger;
import org.netbeans.modules.editor.indent.api.IndentUtils;
import ru.vm5277.common.lexer.SourceBuffer;
import ru.vm5277.lsp.server.ASMLanguageServer;
import ru.vm5277.lsp.server.LSPToken;

public class ASMLspLexerAdapter implements Lexer<ASMTokenId> {
    
    private static final Logger LOGGER = Logger.getLogger(ASMLspLexerAdapter.class.getName());
    
    private			static	ASMLanguageServer				server;
	private	final			LexerRestartInfo<ASMTokenId>	info;
	private					SourceBuffer					sb;
	
    public ASMLspLexerAdapter(LexerRestartInfo<ASMTokenId> info) {
        this.info = info;

		if(null==server) {
			server = new ASMLanguageServer();
            LOGGER.info("ASM LSP server created");
        }
    }
    
	@Override
    public Token<ASMTokenId> nextToken() {
		if(null==sb) {
			int tabSize = IndentUtils.indentLevelSize(null);
			sb = new SourceBuffer(new InputStreamAdapter(info.input()), tabSize);
		}
		LSPToken token = server.readNextToken(sb);
		if(null==token) return null;
		
		System.out.println("TOKEN:" + token.toString());
		return info.tokenFactory().createToken(ASMTokenId.valueOf(token.getType()), token.getLength());
    }
	
    @Override
    public Object state() {
		return null;
    }
    
    @Override
    public void release() {
		try {
			sb.close();
		}
		catch(IOException ex) {
		}
		sb = null;
    }
}
