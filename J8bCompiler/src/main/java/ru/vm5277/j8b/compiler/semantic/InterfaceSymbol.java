/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;

public class InterfaceSymbol extends Symbol {
	private	final	Map<String, List<MethodSymbol>>	methods	= new HashMap<>();

	public InterfaceSymbol(String name) {
		super(name);
	}

	public void addMethod(String name, MethodSymbol method) throws SemanticException {
		List<MethodSymbol> symbols = methods.get(name);
		if(null == symbols) {
			symbols = new ArrayList<>();
			methods.put(name, symbols);
		}
		
		String methodSignature = method.getSignature();
		for(MethodSymbol symbol : symbols) {
			if(methodSignature.equals(symbol.getSignature())) {
				throw new SemanticException("Method '" + methodSignature + "' is already defined in interface '" + name + "'");
			}
		}
		
		symbols.add(method);
	}

	public List<MethodSymbol> getMethods(String name) {
		return methods.get(name);
	}
	
	public Map<String, List<MethodSymbol>> getMethods() {
		return methods;
	}
}