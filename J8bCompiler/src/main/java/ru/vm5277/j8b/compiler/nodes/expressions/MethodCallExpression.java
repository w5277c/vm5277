/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.util.List;
import ru.vm5277.j8b.compiler.SemanticError;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class MethodCallExpression extends ExpressionNode {
	private	final	ExpressionNode			parent;
	private	final	String					methodName;
	private	final	List<ExpressionNode>	arguments;
    
    public MethodCallExpression(TokenBuffer tb, ExpressionNode parent, String methodName, List<ExpressionNode> arguments) {
        super(tb);
        
		this.parent = parent;
		this.methodName = methodName;
        this.arguments = arguments;
    }

	@Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
		return visitor.visit(this);
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public ExpressionNode getParent() {
		return parent;
	}

	public List<ExpressionNode> getArguments() {
		return arguments;
	}
	
	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) {
		throw new SemanticError("Not supported yet.", tb.getSB());
	}
}