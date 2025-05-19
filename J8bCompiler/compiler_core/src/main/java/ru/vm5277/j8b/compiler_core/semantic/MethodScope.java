/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.semantic;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;

public class MethodScope implements Scope {
	private			MethodSymbol		symbol;
	private	final	ClassScope			parent;
	private	final	Map<String, Symbol>	variables		= new HashMap<>();

	public MethodScope(MethodSymbol methodSymbol, ClassScope parent) {
		this.symbol = methodSymbol;
		this.parent = parent;
	}

	public MethodSymbol getSymbol() {
		return symbol;
	}
	public void setSymbol(MethodSymbol symbol) {
		this.symbol = symbol;
	}

	public void addVariable(Symbol symbol) throws SemanticException {
		//TODO нигде не используется
		String name = symbol.getName();

/*TODO перенести в выражения
// Проверяем конфликты с параметрами метода
		for (Symbol param : this.symbol.getParameters()) {
			if (param.getName().equals(name)) Warning  "Variable name '" + name + "' conflicts with parameter name");
		}
*/
		if (variables.containsKey(name)) throw new SemanticException("Duplicate variable name: " + name);

		variables.put(name, symbol);
	}

	@Override
	public Symbol resolve(String name) {
		// 1. Проверяем локальные переменные
		Symbol symbol = variables.get(name);
		if (symbol != null) return symbol;

		// 2. Проверяем параметры метода (через MethodSymbol)
		for (Symbol param : this.symbol.getParameters()) {
			if (param.getName().equals(name)) {
				return param;
			}
		}

		// 3. Делегируем в родительскую область видимости
		return parent != null ? parent.resolve(name) : null;
	}
	
	@Override
	public ClassScope resolveClass(String className) {
		return Scope.resolveClass(this, className);
	}

	@Override
	public InterfaceSymbol resolveInterface(String interfaceName) {
		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolveInterface(interfaceName);
		}

		return null;
	}

	@Override
	public Scope getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}