/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
02.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.common.tokens.Token;

public class MacroSymbol extends Symbol {
	private	int			firstLine;
	private	List<Token> tokens		= new ArrayList<>();
	
	public MacroSymbol(String name, int firstLine) {
		super(name);
		
		this.firstLine = firstLine;
	}
	
	public void addToken(Token token) {
		token.getSP().updateLine(firstLine);
		tokens.add(token);
	}
	
	public List<Token> getTokens() {
		return tokens;
	}
}
