/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.io.IOException;
import java.util.List;
import ru.vm5277.j8b.compiler.nodes.ProgramNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.tokens.Token;

public class Parser {
	private final	List<Token>		tokens;
	
	public Parser(List<Token> tokens) throws IOException {
		this.tokens = tokens;
		
		if(tokens.isEmpty()) return;
		
		ProgramNode program = new ProgramNode(new TokenBuffer(tokens.iterator())); // Создаём корень AST
	}
}

