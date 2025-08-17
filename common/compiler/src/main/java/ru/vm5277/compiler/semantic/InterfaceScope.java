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

public class InterfaceScope extends Scope {
	private			String							name;
	private			Scope							parent;
	private	final	Map<String, List<MethodSymbol>>	methods		= new HashMap<>();
	private	final	Map<String, InterfaceScope>		interfaces	= new HashMap<>();
	
	public InterfaceScope(String name, Scope parentScope) throws CompileException {
		if (name == null || name.isEmpty()) throw new CompileException("Class name cannot be empty");
		this.name = name;
		this.parent = parentScope;
	}

	public void addInterface(InterfaceScope iScope) throws CompileException {
        String symbolName = iScope.getName();
		
		if (interfaces.containsKey(iScope.getName())) throw new CompileException("Interface " + symbolName + " conflicts with interface of the same name");
		interfaces.put(symbolName, iScope);
	}

	public void addMethod(MethodSymbol method) throws CompileException {
		String symbolName = method.getName();
		if (interfaces.containsKey(symbolName)) throw new CompileException("Method name '" + symbolName + "' conflicts with interface name");
		
		List<MethodSymbol> symbols = methods.get(symbolName);
		if(null == symbols) {
			symbols = new ArrayList<>();
			methods.put(symbolName, symbols);
		}
		
		String methodSignature = method.getSignature();
		for(MethodSymbol symbol : symbols) {
			if(methodSignature.equals(symbol.getSignature())) {
				throw new CompileException("Method '" + methodSignature + "' is already defined in interface '" + symbolName + "'");
			}
		}
		
		symbols.add(method);
	}

	public Map<String, InterfaceScope> getInterfaces() {
		return interfaces;
	}

	public List<MethodSymbol> getMethods(String name) {
		return methods.get(name);
	}
	
	public Map<String, List<MethodSymbol>> getMethods() {
		return methods;
	}

	@Override
	public Symbol resolve(String name) {
		// Поиск методов (без параметров - для простых случаев)
		if (methods.containsKey(name) && !methods.get(name).isEmpty()) {
			// Возвращаем первый метод с таким именем (для точного поиска нужно использовать resolveMethod)
			return methods.get(name).get(0);
		}

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolve(name);
		}
		
		// Символ не найден
		return null;
	}

	public InterfaceScope resolveInterface(String interfaceName) {
		// Поиск в текущем классе
		if (interfaces.containsKey(interfaceName)) return interfaces.get(interfaceName);

		// Поиск в импортах (если интерфейс объявлен в другом пакете)
//		String importedName = imports.get(interfaceName);
//		if (null != importedName && interfaces.containsKey(importedName)) return interfaces.get(importedName);

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			if(parent instanceof ClassScope) {
				return ((ClassScope)parent).resolveInterface(interfaceName);
			}
			return ((InterfaceScope)parent).resolveInterface(interfaceName);
		}

		return null;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public Scope getParent() {
		return parent;
	}

	@Override
	public ClassScope getThis() {
		return parent.getThis();
	}
}