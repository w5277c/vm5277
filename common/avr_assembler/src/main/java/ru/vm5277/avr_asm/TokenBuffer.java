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

import java.util.Iterator;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.avr_asm.tokens.Token;
import ru.vm5277.common.Operator;

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

	public boolean match(TokenType type, Keyword keyword) {
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
