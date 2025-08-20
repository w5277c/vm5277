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

import ru.vm5277.common.compiler.VarType;

public class ClassSymbol extends Symbol {
	private	final	Scope	scope;
	
	public ClassSymbol(String name, VarType returnType, boolean isFinal, boolean isStatic, Scope scope) {
		super(name, returnType, isFinal, isStatic);
		
		this.scope = scope;
	}

	public Scope getScope() {
		return scope;
	}
	
	@Override
	public String toString() {
		return	((isNative ? "native " : "") + (isFinal ? "final " : "") + (isStatic ? "static " : "") + " " +
				(null == ((ClassScope)scope.getParent()).getName() ? name : ((ClassScope)scope.getParent()).getName() + "." + name)).trim();
	}
}