/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class VariableExpression extends ExpressionNode {
    private final	String	value;
    private			Symbol	resolvedSymbol;
	
    public VariableExpression(TokenBuffer tb, MessageContainer mc, String value) {
        super(tb, mc);
        
		this.value = value;
    }
    
	public String getValue() {
		return value;
	}
	
	@Override
	public VarType getType(Scope scope) throws SemanticException {
		if (resolvedSymbol == null) {
            resolvedSymbol = scope.resolve(value);
        }
        return resolvedSymbol.getType();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + value;
	}
	
	@Override
	public boolean preAnalyze() {
		if (null == value || value.isEmpty()) {
			markError("Variable name cannot be empty");
			return false;
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		if (resolvedSymbol == null) {
            resolvedSymbol = scope.resolve(value);
        }
		if (null == resolvedSymbol) {
			markError("Variable '" + value + "' is not declared");
			return false;
		}
		return true;
	}
}