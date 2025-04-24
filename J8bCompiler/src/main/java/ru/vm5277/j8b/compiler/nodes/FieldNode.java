/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import java.util.Set;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;

public class FieldNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			VarType			returnType;
	private			String			name;
	private	final	ExpressionNode	initializer;
	
	public FieldNode(TokenBuffer tb, Set<Keyword> modifiers, VarType type, String name) {
		super(tb);

		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            initializer = null;
        }
		else {
			tb.consume();
			initializer = new ExpressionParser(tb).parse();
		}
        tb.consume(Delimiter.SEMICOLON);
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	public VarType getType() {
		return returnType;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return initializer;
	}
}
