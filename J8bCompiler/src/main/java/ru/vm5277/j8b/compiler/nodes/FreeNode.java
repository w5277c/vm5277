/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionParser;

public class FreeNode extends AstNode {
	private final ExpressionNode target;

	public FreeNode(TokenBuffer tb) {
		super(tb);

		tb.consume();
		this.target = new ExpressionParser(tb).parse();
		tb.consume(Delimiter.SEMICOLON);
	}

	@Override
	public String toString() {
		return "free: " + target;
	}
}