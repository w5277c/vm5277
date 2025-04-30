/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.expressions;

import java.math.BigInteger;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.SymbolTable;

public class LiteralExpression extends ExpressionNode {
    private final Object value;
    
    public LiteralExpression(TokenBuffer tb, Object value) {
        super(tb);
        
		this.value = value;
    }
    
	@Override
	public VarType semanticAnalyze(SymbolTable symbolTable) {
		if (value instanceof Integer) return VarType.INT;
		if (value instanceof Boolean) return VarType.BOOL;
		if (value instanceof String) return VarType.STRING;
		return VarType.UNKNOWN;
	}
	
    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    public Object getValue() {
		return value;
	}
	
	public boolean isInteger() {
		return value instanceof Integer || value instanceof Long || value instanceof BigInteger;
	}
	
	public long toLong() {
		return ((Number)value).longValue();
	}
	
	@Override
	public String toString() {
        if (null == value) return getClass().getSimpleName() + ":null";
		if(value instanceof Double) return getClass().getSimpleName() + ":" + ((Double)value).toString();
		if(value instanceof Number) return getClass().getSimpleName() + ":" + ((Number)value).toString();
		if(value instanceof String) return getClass().getSimpleName() + ":" + ((String)value);
		return getClass().getSimpleName() + ":" + value;
    }
}