/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;

public class BlockNode extends AstNode {
	private	List<AstNode>		declarations	= new ArrayList<>();
	
	public BlockNode(TokenBuffer tb, AstNode singleStatement) {
		super(tb);
		
		declarations.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb, String className) {
        super(tb);
        
		tb.consume(Delimiter.LEFT_BRACE);
        while (!tb.match(Delimiter.RIGHT_BRACE) && !tb.match(TokenType.EOF)) {
            Set<Keyword> modifiers = collectModifiers();
            
			// Обработка классов с модификаторами
			if (null != className && tb.match(TokenType.OOP) && Keyword.CLASS == tb.current().getValue()) {
				declarations.add(new ClassNode(tb, modifiers, className));
				continue;
			}
			
			VarType type = null;
			String name = null;
			if(tb.match(TokenType.TYPE)) {
				type = VarType.fromKeyword((Keyword)tb.current().getValue());
				tb.consume();
			}
			if(tb.match(TokenType.ID)) {
				name = tb.current().getValue().toString();
				tb.consume();
			}
			
            if (!className.isEmpty() && null == type && className.equals(name)) {
				declarations.add(new MethodNode(tb, modifiers, null, name));
				continue;
			}
			
			if(null != type && null != name) {
				declarations.add(tb.match(Delimiter.LEFT_PAREN) ? new MethodNode(tb, modifiers, type, name) : new FieldNode(tb, modifiers, type, name));
				continue;
            }

			AstNode statement = parseStatement();
			declarations.add(statement);

			// Собираем вложенные блоки
			if(statement instanceof BlockNode) {
				blocks.add((BlockNode)statement);
			}
        }
        tb.consume(Delimiter.RIGHT_BRACE);
    }
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
}