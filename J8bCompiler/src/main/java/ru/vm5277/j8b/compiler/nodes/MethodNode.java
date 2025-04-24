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
import ru.vm5277.j8b.compiler.enums.VarType;

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	VarType				returnType;
	private	final	String				name;
	private			List<ParameterNode>	parameters;
	
	public MethodNode(TokenBuffer tb, Set<Keyword> modifiers, VarType returnType, String name) {
		super(tb);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
		
		this.parameters = parseParameters();

		blocks.add(new BlockNode(tb, ""));
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
	
	public BlockNode getBody() {
		return blocks.get(0);
	}
	
	public boolean isConstructor() {
		return null == returnType;
	}

	public List<ParameterNode> getParameters() {
		return parameters;
	}
	
	public VarType getType() {
		return returnType;
	}

	public String getName() {
		return name;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
}
