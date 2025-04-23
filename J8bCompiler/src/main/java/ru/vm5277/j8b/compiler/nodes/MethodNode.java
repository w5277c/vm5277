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

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	Keyword				returnType;
	private	final	String				name;
	private			List<ParameterNode>	parameters;
	private	final	BlockNode			body;
	
	public MethodNode(TokenBuffer tb, Set<Keyword> modifiers, Keyword returnType, String name) {
		super(tb);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
		
		this.parameters = parseParameters();
		this.body = new BlockNode(tb);
	}
	
	private List<ParameterNode> parseParameters() {
        List<ParameterNode> params = new ArrayList<>();
        tb.consume(Delimiter.LEFT_PAREN);
        
        if (!tb.match(Delimiter.RIGHT_PAREN)) {
            while(true) {
                params.add(new ParameterNode(tb));
                if (!tb.match(Delimiter.COMMA)) break;
                tb.consume(Delimiter.COMMA);
            }
        }
        
        tb.consume(Delimiter.RIGHT_PAREN);
        return params;
    }
}
