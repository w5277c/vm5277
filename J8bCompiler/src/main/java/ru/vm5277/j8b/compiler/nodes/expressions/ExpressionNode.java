/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;

public abstract class ExpressionNode extends AstNode {
	public ExpressionNode(TokenBuffer tb) {
		super(tb);
		
	}
	
	// Для визитора (понадобится при кодогенерации)
    public abstract <T> T accept(ExpressionVisitor<T> visitor);
}
