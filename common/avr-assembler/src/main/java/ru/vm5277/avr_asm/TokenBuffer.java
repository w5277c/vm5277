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
import java.util.Iterator;
import ru.vm5277.common.lexer.SourcePosition;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.lexer.TokenType;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.lexer.tokens.Token;

public class TokenBuffer {
	private	Token	current;
	private	final	Iterator<Token>		iterator;
	
	public TokenBuffer(Iterator<Token> iterator) {
		this.iterator = iterator;
		this.current = iterator.hasNext() ? iterator.next() : new Token(null, TokenType.EOF, (Object)null);
	}
	
	public Token consume() { // Или next
		Token result = current;
		current =  iterator.hasNext() ? iterator.next() : new Token(null, TokenType.EOF, null);
		return result;
	}
	
	public Token current() {
		return current;
	}
	
	public boolean match(TokenType type) {
        return current.getType() == type;
    }

	public boolean match(TokenType type, ASMKeyword keyword) {
        return type == current.getType() && current.getValue() == keyword;
    }

	public boolean match(Delimiter delimiter) {
        return TokenType.DELIMITER == current.getType() && current.getValue() == delimiter;
    }
	
	public boolean match(Operator operator) {
        return TokenType.OPERATOR == current.getType() && current.getValue() == operator;
    }

	public SourcePosition getSP() {
		return current.getSP();
	}
	
	public void skipLine() {
		while(!match(TokenType.NEWLINE) && !match(TokenType.EOF)) {
			consume();
		}
	}
}
