/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.enums.Delimiter;

public class BlockNode extends AstNode {
	private	List<AstNode>		declarations	= new ArrayList<>();
	
	public BlockNode(TokenBuffer tb, AstNode singleStatement) {
		super(tb);
		
		declarations.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb, String className) {
        super(tb);
        
		tb.consume(Delimiter.LEFT_BRACE);
		parseBody(declarations, className);
		tb.consume(Delimiter.RIGHT_BRACE);
    }
	
	public List<AstNode> getDeclarations() {
		return declarations;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}