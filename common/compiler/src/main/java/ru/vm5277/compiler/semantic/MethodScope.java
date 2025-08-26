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

public class MethodScope extends BlockScope {
	private	MethodSymbol	mSymbol;
	
	public MethodScope(MethodSymbol mSymbol, Scope parent) {
		super(parent);
		this.mSymbol = mSymbol;
	}

	public MethodSymbol getSymbol() {
		return mSymbol;
	}
	public void setSymbol(MethodSymbol mSymbol) {
		this.mSymbol = mSymbol;
	}

//	Убрал, так как отличается от BlockScope только поиском в параметрах, что не верно, параметры не имеют прямого отношения к переданным переменным
	@Override
	public Symbol resolveSymbol(String name) {
		// 1. Проверяем локальные переменные
		Symbol symbol = variables.get(name);
		if (symbol != null) return symbol;

		// 2. Проверяем параметры метода (через MethodSymbol)
		for (Symbol param : mSymbol.getParameters()) {
			if (param.getName().equals(name)) {
				return param;
			}
		}

		// 3. Делегируем в родительскую область видимости
		return parent != null ? parent.resolveSymbol(name) : null;
	}
	
	@Override
	public String toString() {
		return mSymbol.toString();
	}
}