/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.exceptions.SemanticException;

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
	public ClassScope getThis() {
		return parent;
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