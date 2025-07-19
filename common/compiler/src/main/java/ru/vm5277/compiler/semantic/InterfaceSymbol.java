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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.exceptions.CompileException;

public class InterfaceSymbol extends Symbol {
	private	final	Map<String, List<MethodSymbol>>	methods	= new HashMap<>();

	public InterfaceSymbol(String name) {
		super(name);
	}

	public void addMethod(String name, MethodSymbol method) throws CompileException {
		List<MethodSymbol> symbols = methods.get(name);
		if(null == symbols) {
			symbols = new ArrayList<>();
			methods.put(name, symbols);
		}
		
		String methodSignature = method.getSignature();
		for(MethodSymbol symbol : symbols) {
			if(methodSignature.equals(symbol.getSignature())) {
				throw new CompileException("Method '" + methodSignature + "' is already defined in interface '" + name + "'");
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