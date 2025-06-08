/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
31.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.semantic;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.scope.VariableSymbol;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.MessageContainer;

public class VariableExpression extends Expression {
    private	Scope	scope;
	private	String	name;
	private	Long	value;
	
    public VariableExpression(TokenBuffer tb, Scope scope, MessageContainer mc, String name) throws ParseException {
        this.scope = scope;
		this.name = name;
    }
    
	public Long getValue() throws ParseException {
		if(null == value) {
			VariableSymbol symbol = scope.resolveVariable(name);
			if(null != symbol) {
				value = symbol.getValue();
			}
			else {
				Integer addr = scope.resolveLabel(name);
				if(null != addr) value = (long)addr;
			}
		}
		return value;
	}
	
	@Override
	public String toString() {
		return name;
	}
}