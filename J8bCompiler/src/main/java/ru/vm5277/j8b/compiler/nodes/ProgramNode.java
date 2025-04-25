/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;

public class ProgramNode extends AstNode {
	private	List<AstNode> declarations = new ArrayList<>(); // Импорты, глобальные переменные, функции, классы
	
	public ProgramNode(TokenBuffer tb) {
		super(tb);

		// Обработка импортов		
		while (tb.match(Keyword.IMPORT) && !tb.match(TokenType.EOF)) {
			declarations.add(new ImportNode(tb));
		}
		parseBody(declarations, null);
	}        
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
}
