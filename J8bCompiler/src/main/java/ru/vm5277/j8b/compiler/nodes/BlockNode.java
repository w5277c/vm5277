/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.vm5277.j8b.compiler.tokens.enums.Delimiter;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.enums.TokenType;

public class BlockNode extends AstNode {
	private	List<FieldNode>		fields		= new ArrayList<>();
	private	List<MethodNode>	methods		= new ArrayList<>();
	private	List<AstNode>		statements	= new ArrayList<>();
	
	public BlockNode(TokenBuffer tb, AstNode singleStatement) {
		super(tb);
		
		statements.add(singleStatement);
	}

	public BlockNode(TokenBuffer tb) {
        super(tb);
        
		tb.consume(Delimiter.LEFT_BRACE);
        while (!tb.match(Delimiter.RIGHT_BRACE) && !tb.match(TokenType.EOF)) {
            Set<Keyword> modifiers = collectModifiers();
            
            if (tb.match(TokenType.TYPE)) {
                Keyword type = (Keyword)tb.current().getValue();
                tb.consume(TokenType.TYPE);
                
				String name = tb.consume(TokenType.ID).getValue().toString();
                
                if (tb.match(Delimiter.LEFT_PAREN)) {
                    methods.add(new MethodNode(tb, modifiers, type, name));
                }
				else {
					fields.add(new FieldNode(tb, modifiers, type, name));
				}
            }
			else {
				statements.add(parseStatement());
			}
        }
        tb.consume(Delimiter.RIGHT_BRACE);
    }
}