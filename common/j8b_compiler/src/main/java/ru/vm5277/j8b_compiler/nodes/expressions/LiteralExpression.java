/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.expressions;

import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.common.j8b_compiler.Operand;
import ru.vm5277.common.j8b_compiler.OperandType;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class LiteralExpression extends ExpressionNode {
    private Object value;
    
    public LiteralExpression(TokenBuffer tb, MessageContainer mc, Object value) {
        super(tb, mc);
        
		this.value = value;
    }
    
	@Override
	public VarType getType(Scope scope) {
		if (value == null) return VarType.NULL;
		if (value instanceof Boolean) return VarType.BOOL;
		if (value instanceof Number)  {
			if(value instanceof Double) return VarType.FIXED;
			
			long l = ((Number)value).longValue();
			if(l<0) return VarType.FIXED;
			if(l<=255) return VarType.BYTE;
			if(l<=65535) return VarType.SHORT;
			return VarType.INT;
		}
		if (value instanceof String) return VarType.CSTR;
		return VarType.UNKNOWN;
	}	

	public Object getValue() {
		return value;
	}
	
	public boolean isInteger() {
		return value instanceof Integer || value instanceof Long;
	}
	
	public boolean isCstr() {
		return value instanceof String;
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
	
	@Override
	public boolean preAnalyze() {
		if (value == null) {
			markError("Literal value cannot be null");
			return false;
		}
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		try {
			if (value instanceof Number) {
				VarType type = getType(scope);
				if (type == VarType.UNKNOWN) {
					markError("Unsupported numeric literal type");
					return false;
				}
				// Проверка диапазона через VarType.checkRange
				type.checkRange((Number)value);
			}
			return true;
		} 
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		cg.setAcc(new Operand(getType(null).getId(), OperandType.LITERAL, value));
	}
}