/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
29.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.common;

import java.util.HashMap;
import java.util.Map;

public class Keyword {
	private	static	final	Map<String, Keyword>	KEYWORDS	= new HashMap<>();	

	// Литералы
	public	static	final	Keyword					TRUE		= new Keyword("true", TokenType.LITERAL);
	public	static	final	Keyword					FALSE		= new Keyword("false", TokenType.LITERAL);
	public	static	final	Keyword					NULL		= new Keyword("null", TokenType.LITERAL);

	static {
		AsmKeyword.BYTE.getName();
		J8bKeyword.AS.getName();
	}
	
	private	String		name;
	private	TokenType	tokenType;
	
	protected Keyword(String name, TokenType tokenType) {
		this.name = name;
		this.tokenType = tokenType;
		
		KEYWORDS.put(name, this);
	}
	
	public String getName() {
		return name;
	}
	
	public TokenType getTokenType() {
		return tokenType;
	}
	
	public static Keyword fromString(String name) {
		return KEYWORDS.get(name);
	}
	
	public static int getSize() {
		return KEYWORDS.size();
	}
	
	@Override
	public String toString() {
		return name;
	}
}