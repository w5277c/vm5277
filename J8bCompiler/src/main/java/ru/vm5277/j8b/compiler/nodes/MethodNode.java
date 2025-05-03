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
import ru.vm5277.j8b.compiler.exceptions.CriticalParseException;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.messages.ErrorMessage;

public class MethodNode extends AstNode {
	private	final	Set<Keyword>		modifiers;
	private	final	VarType				returnType;
	private	final	String				name;
	private			List<ParameterNode>	parameters;
	
	public MethodNode(TokenBuffer tb, Set<Keyword> modifiers, VarType returnType, String name) throws ParseException {
		super(tb);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;

        consumeToken(tb); // Потребляем '('
		this.parameters = parseParameters();
        consumeToken(tb); // Потребляем ')'
		
		if(tb.match(Delimiter.LEFT_BRACE)) {
			tb.getLoopStack().add(this);
			try {blocks.add(new BlockNode(tb));}catch(ParseException e) {}
			tb.getLoopStack().remove(this);
		}
		else {
			markFirstError(tb.error("Method '" + name + "' must contain a body"));
		}
	}
	
	private List<ParameterNode> parseParameters() {
        List<ParameterNode> params = new ArrayList<>();
        
		while(!tb.match(TokenType.EOF) && !tb.match(Delimiter.RIGHT_PAREN)) {
			try {
				params.add(new ParameterNode(tb));
				if (tb.match(Delimiter.COMMA)) {
					consumeToken(tb); // Потребляем ','
					continue;
				}
				else if(tb.match(TokenType.EOF) || tb.match(Delimiter.RIGHT_PAREN)) {
					break;
				}
				ErrorMessage message = new ErrorMessage("Expected " + Delimiter.RIGHT_PAREN + ", but got " + tb.current().getType(), tb.current().getSP());
				tb.addMessage(message);
				markFirstError(message);
				break;
			}
			catch(ParseException e) {
				markFirstError(e);
				tb.skip(Delimiter.RIGHT_PAREN);
				break;
			}
		}

		return params;
    }
	
	public BlockNode getBody() {
		return blocks.isEmpty() ? null : blocks.get(0);
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
