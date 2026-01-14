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

import org.netbeans.api.lexer.Language;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;
import java.util.Collection;
import java.util.EnumSet;

public class ASMLexerFactory extends LanguageHierarchy<ASMTokenId> {
    
    private static final Language<ASMTokenId> LANGUAGE = new ASMLexerFactory().language();
    
    public static Language<ASMTokenId> getLanguage() {
        return LANGUAGE;
    }
    
    @Override
    protected Collection<ASMTokenId> createTokenIds() {
        return EnumSet.allOf(ASMTokenId.class);
    }
    
    @Override
    protected Lexer<ASMTokenId> createLexer(LexerRestartInfo<ASMTokenId> info) {
		return new ASMLspLexerAdapter(info);
    }
    
    @Override
    protected String mimeType() {
        return "text/x-vm5277-asm";
    }
}