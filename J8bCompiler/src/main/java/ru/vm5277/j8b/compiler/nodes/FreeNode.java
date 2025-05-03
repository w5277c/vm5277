/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;

public class FreeNode extends AstNode {
	private ExpressionNode target;

	public FreeNode(TokenBuffer tb) {
		super(tb);

		consumeToken(tb); // Наличие гарантировано вызывающим
		try {this.target = new ExpressionNode(tb).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	@Override
	public String toString() {
		return "free: " + target;
	}
}