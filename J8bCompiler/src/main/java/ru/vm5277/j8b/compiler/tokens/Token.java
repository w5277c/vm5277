/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import ru.vm5277.j8b.compiler.enums.TokenType;
import java.math.BigDecimal;
import ru.vm5277.j8b.compiler.ParseError;

public class Token {
	protected	TokenType	type;
	protected	Object		value;
	protected	int			line;
	protected	int			column;
	protected	int			endPos;
	private		ParseError	error;
	
	public Token() {
	}
	
	public Token(TokenType type, Object value, ParseError error) {
		this.type = type;
		this.value = value;
		this.line = error.getLine();
		this.column = error.getColumn();
		this.error = error;
	}

	public Token(TokenType type, Object value, int line, int column) {
		this.type = type;
		this.value = value;
		this.line = line;
		this.column = column;
	}

	// Вспомогательные методы для доступа к значениям
	public Integer intValue() {
		return (Integer)value;
	}
    
	public BigDecimal decimalValue() {
		return (BigDecimal)value;
	}

	public Boolean booleanValue() {
		return (Boolean)value;
	}

	public String stringValue() {
		return (String)value;
	}
	
	
	public int getEndPos() {
		return endPos;
	}
	
	public Object getValue() {
		return value;
	}
	
	public TokenType getType() {
		return type;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getColumn() {
		return column;
	}
	
	public ParseError getError() {
		return error;
	}
	
	@Override
	public String toString() {
		return type + "(" + value + ")[" + line + ":" + column + "]";
	}
	
	public static Token parse() {
		return null;
	};
}
