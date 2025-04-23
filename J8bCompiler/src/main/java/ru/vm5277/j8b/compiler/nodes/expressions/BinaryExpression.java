/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.tokens.enums.Operator;

public class BinaryExpression extends ExpressionNode {
    private final ExpressionNode	left;
    private final Token				operator;
    private final ExpressionNode	right;
    
    public BinaryExpression(TokenBuffer tb, ExpressionNode left, Token operator, ExpressionNode right) {
        super(tb);
        
		this.left = left;
        this.operator = operator;
        this.right = right;
    }
    
	// Для визитора (понадобится при кодогенерации)
	@Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}