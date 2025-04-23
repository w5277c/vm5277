/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.nodes.TokenBuffer;

public class LiteralExpression extends ExpressionNode {
    private final Object value;
    
    public LiteralExpression(TokenBuffer tb, Object value) {
        super(tb);
        
		this.value = value;
    }
    
    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public Object getValue() {
        return value;
    }
}