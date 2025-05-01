/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.List;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class NewExpression extends ExpressionNode {
	private	String					className;
	private	List<ExpressionNode>	args;
	
	public NewExpression(TokenBuffer tb, String className, List<ExpressionNode> args) {
        super(tb);
        
		this.className = className;
		this.args = args;
    }
    
    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) {
		return null;
	}
}