/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.List;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;

public class MethodCallExpression extends ExpressionNode {
	private	final	ExpressionNode			target;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	arguments;
    
    public MethodCallExpression(TokenBuffer tb, ExpressionNode target, String methodName, List<ExpressionNode> arguments) {
        super(tb);
        
		this.target = target;
		this.methodName = methodName;
        this.arguments = arguments;
    }
    
	@Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
		return visitor.visit(this);
	}
}