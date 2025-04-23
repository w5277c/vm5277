/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.tokens.Token;

public class UnaryExpression extends ExpressionNode {
    private final Token operator;
    private final ExpressionNode operand;
    
    public UnaryExpression(TokenBuffer tb, Token operator, ExpressionNode operand) {
        super(tb);
        this.operator = operator;
        this.operand = operand;
    }
    
    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}