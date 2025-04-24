/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class ProgramNode extends AstNode {
	private	List<AstNode> declarations = new ArrayList<>(); // Импорты, глобальные переменные, функции, классы
	
	public ProgramNode(TokenBuffer tb) {
		super(tb);
		
		while (TokenType.EOF != tb.current().getType()) {
			// 1. Обработка импортов
			if (tb.match(TokenType.KEYWORD) && Keyword.IMPORT == tb.current().getValue()) {
				declarations.add(new ImportNode(tb));
				continue;
			}
        
			Set<Keyword> modifiers = collectModifiers();

			// 2. Обработка классов с модификаторами
			if (tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
				declarations.add(new ClassNode(tb, modifiers, null));
				continue;
			}

			// 3. Обработка функций и глобальных переменных
			if (tb.match(TokenType.TYPE)) {
				VarType type = VarType.fromKeyword((Keyword)tb.current().getValue());
				tb.consume();
				Token nameToken = tb.consume(TokenType.ID);
            
				if(tb.match(TokenType.DELIMITER) && Delimiter.LEFT_PAREN == tb.current().getValue()) { // Это функция
					declarations.add(new MethodNode(tb, modifiers, type, (String)nameToken.getValue()));
				}
				else { // Глобальная переменная
					declarations.add(new FieldNode(tb, modifiers, type, (String)nameToken.getValue()));
				}
				continue;
            }
			
			// 4. Обработка остальных statement (if, while, вызовы и т.д.)
			AstNode statement = parseStatement();
			declarations.add(statement);
			if(statement instanceof BlockNode) {
				blocks.add((BlockNode)statement);
			}
        }
	}        
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
}
