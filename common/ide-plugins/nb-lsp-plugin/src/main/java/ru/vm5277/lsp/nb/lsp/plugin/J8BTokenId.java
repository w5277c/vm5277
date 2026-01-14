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

package ru.vm5277.lsp.nb.lsp.plugin;

import org.netbeans.api.lexer.TokenId;

public enum J8BTokenId implements TokenId {
    LITERAL("keyword"),
    TYPE("keyword"),
    COMMAND("keyword"),
    MODIFIER("keyword"),
    OOP("keyword"),
    KEYWORD("keyword"),
    DIRECTIVE("keyword"),
    MNEMONIC("keyword"),
    
    IDENTIFIER("identifier"),
    INDEX_REG("keyword"),
    LABEL("keyword"),
    NUMBER("number"),
    NOTE("string"),
    OPERATOR("operator"),
    DELIMITER("separator"),
    STRING("string"),
    CHARACTER("char"),
    MACRO_PARAM("keyword"),
    
	INVALID("error"),
	WHITESPACE("whitespace"),
	COMMENT("whitespace"),
	NEWLINE("whitespace"),
    EOF("eof");
	
    private final String category;
    
    J8BTokenId(String category) {
        this.category = category;
    }
    
    @Override
    public String primaryCategory() {
        return category;
    }
	
	public static J8BTokenId fromString(String tokenType) {
		try {
			return J8BTokenId.valueOf(tokenType.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return IDENTIFIER;
		}
	}
}