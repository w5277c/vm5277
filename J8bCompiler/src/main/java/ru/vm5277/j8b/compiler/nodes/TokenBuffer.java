/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.Iterator;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class TokenBuffer {
	private	Token	current;
	private	final	Iterator<Token> iterator;
	
	public TokenBuffer(Iterator<Token> iterator) {
		this.iterator = iterator;
		this.current = iterator.hasNext() ? iterator.next() : new Token(TokenType.EOF, null, -1, -1);
	}
	
	public Token consume() { // Или next
		Token result = current;
		current =  iterator.hasNext() ? iterator.next() : new Token(TokenType.EOF, null, current.getLine(), current.getColumn());
		return result;
	}
	
	public Token current() {
		return current;
	}
	
	public Token consume(TokenType expectedType) {
		if (current.getType() == expectedType) {
            return consume();
        }
        throw new ParseError("Expected " + expectedType + ", but got " + current.getType(), current.getLine(), current.getColumn());
    }
	
	public Token consume(Delimiter delimiter) {
		if (TokenType.DELIMITER == current.getType()) {
            if(delimiter == current.getValue()) {
				return consume();
			}
			else {
				throw new ParseError("Expected delimiter " + delimiter + ", but got " + current.getValue(), current.getLine(), current.getColumn());
			}
        }
        throw new ParseError("Expected " + TokenType.DELIMITER + ", but got " + current.getType(), current.getLine(), current.getColumn());
    }

	public Token consume(TokenType type, Keyword keyword) {
		if (type == current.getType()) {
            if(keyword == current.getValue()) {
				return consume();
			}
			else {
				throw new ParseError("Expected keyword " + keyword + ", but got " + current.getValue(), current.getLine(), current.getColumn());
			}
        }
        throw new ParseError("Expected " + TokenType.KEYWORD + ", but got " + current.getType(), current.getLine(), current.getColumn());
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
}
