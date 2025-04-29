/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javax.xml.bind.DatatypeConverter;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.ParseError;
import ru.vm5277.j8b.compiler.enums.Keyword;

public class Token {
	private		final	static	DecimalFormat	df	= new DecimalFormat("0.####################", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	protected					TokenType	type;
	protected					Object		value;
	protected					int			line;
	protected					int			column;
	protected					int			endPos;
	private						ParseError	error;
	
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

	public int getEndPos() {
		return endPos;
	}
	
	public Object getValue() {
		if(TokenType.LITERAL == type) {
			switch((Keyword)value) {
				case TRUE: return true;
				case FALSE: return false;
				case NULL: return null;
			}
		}
		return value;
	}
	
	public String getStringValue() {
		return toStringValue(value);
	}
	
	public static String toStringValue(Object value) {
		if(value instanceof Double) return Token.df.format((Double)value);
		if(value instanceof Number) return ((Number)value).toString();
		if(value instanceof Boolean) return ((Boolean)value).toString();
		if(value instanceof byte[]) return "0x" + DatatypeConverter.printHexBinary((byte[])value);
		return (String)value;
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
