/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.Iterator;
import java.util.Stack;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.SourcePosition;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.messages.ErrorMessage;
import ru.vm5277.j8b.compiler.messages.Message;
import ru.vm5277.j8b.compiler.messages.MessageContainer;

public class TokenBuffer {
	private	Token	current;
	private	final	Iterator<Token>		iterator;
	private	final	MessageContainer	mc;
	private	final	Stack<AstNode>		loopStack	= new Stack<>();
	
	public TokenBuffer(Iterator<Token> iterator, MessageContainer mc) {
		this.iterator = iterator;
		this.mc = mc;
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
	
	public Stack<AstNode> getLoopStack() {
		return loopStack;
	}
	
	public SourcePosition getSP() {
		return current.getSP();
	}
	
	public ParseException error(String text) {
		ErrorMessage message = new ErrorMessage(text, current.getSP());
		addMessage(message);
		return new ParseException(message);
	}
	public void addMessage(Message message) {
		mc.add(message);
	}
	
	public void skip(Delimiter... delimiters) {
		while(!match(TokenType.EOF)) {
			Token token = consume();
			for(Delimiter delimiter : delimiters) {
				if(TokenType.DELIMITER == token.getType() && token.getValue() == delimiter) return;
			}
		}
	}
}
