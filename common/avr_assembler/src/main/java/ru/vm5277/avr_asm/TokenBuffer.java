/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

import java.util.Iterator;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.tokens.Token;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.TokenType;

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

	public boolean match(Keyword keyword) {
        return TokenType.KEYWORD == current.getType() && current.getValue() == keyword;
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
