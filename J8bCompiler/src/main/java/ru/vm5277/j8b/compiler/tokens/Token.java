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
import ru.vm5277.j8b.compiler.SourceBuffer;
import ru.vm5277.j8b.compiler.enums.Keyword;

public class Token {
	private		final	static	DecimalFormat	df	= new DecimalFormat("0.####################", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
	protected					TokenType		type;
	protected					Object			value;
	protected					SourceBuffer	sb;
	private						ParseError		error;
	
	public Token(SourceBuffer sb) {
		this.sb = sb;
	}
	
	public Token(TokenType type, Object value, ParseError error) {
		this.type = type;
		this.value = value;
		this.sb = (null == error ? null : error.getSP());
		this.error = error;
	}

	public Token(SourceBuffer sb, TokenType type, Object value) {
		this.type = type;
		this.value = value;
		this.sb = sb;
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
	
	public SourceBuffer getSB() {
		return sb;
	}
	
	public ParseError getError() {
		return error;
	}
	
	@Override
	public String toString() {
		return type + "(" + value + ")" + sb;
	}
}
