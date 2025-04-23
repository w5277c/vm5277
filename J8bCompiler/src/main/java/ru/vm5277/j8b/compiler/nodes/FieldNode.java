/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import java.util.Set;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;
import ru.vm5277.j8b.compiler.tokens.enums.Delimiter;
import ru.vm5277.j8b.compiler.tokens.enums.Keyword;
import ru.vm5277.j8b.compiler.tokens.enums.Operator;

public class FieldNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			Keyword			returnType;
	private			String			name;
	private	final	ExpressionNode	initializer;
	
	public FieldNode(TokenBuffer tb, Set<Keyword> modifiers, Keyword type, String name) {
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
}
