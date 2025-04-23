/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.nodes.TokenBuffer;

public class VariableExpression extends ExpressionNode {
    private final String name;
    
    public VariableExpression(TokenBuffer tb, String name) {
        super(tb);
        
		this.name = name;
    }
    
    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}