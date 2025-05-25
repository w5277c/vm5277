/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.semantic;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler.common.enums.VarType;

public class MethodSymbol extends Symbol {
	private	final	List<Symbol>	parameters;
	private			boolean			canThrow;
	private			String			signature;
	private	final	MethodScope		scope;
	
	public MethodSymbol(String name, VarType returnType, List<Symbol> parameters, boolean isFinal, boolean isStatic, boolean isNative, boolean canThrow,
						MethodScope scope) {
		super(name, returnType, isFinal, isStatic, isNative);
		
		this.parameters = parameters;
		this.canThrow = canThrow;
		this.scope = scope;
	}

	public MethodScope getScope() {
		return scope;
	}
	
	public List<Symbol> getParameters() {
		return parameters;
	}
	
	public List<VarType> getParameterTypes() {
		List<VarType> result = new ArrayList<>();
		for (Symbol param : parameters) {
			result.add(param.getType());
		}
		return result;
	}
	
	public boolean canThrow() {
		return canThrow;
	}
	
	public synchronized String getSignature() {
		if(null == signature) {
			StringBuilder sb = new StringBuilder();
			sb.append(name).append("(");
			for (Symbol param : parameters) {
				sb.append(param.getType().getName()).append(",");
			}
			if (!parameters.isEmpty()) {
				sb.setLength(sb.length() - 1); // Удаляем последнюю запятую
			}
			sb.append(")");
			signature = sb.toString();
		}
		return signature;
	}
}