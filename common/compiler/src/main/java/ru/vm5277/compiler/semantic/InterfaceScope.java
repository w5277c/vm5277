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
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.AstNode;

public class InterfaceScope extends Scope {
	protected			String							name;
	protected			Scope							parent;
	protected	final	Map<String, String>				imports		= new HashMap<>();
	protected			List<VarType>					impl		= null;
	protected	final	List<MethodSymbol>				methods		= new ArrayList<>();
	protected	final	Map<String, InterfaceScope>		interfaces	= new HashMap<>();
	
	protected InterfaceScope() {
	}
			
	public InterfaceScope(String name, Scope parentScope, List<VarType> impl) throws CompileException {
		if (name == null || name.isEmpty()) throw new CompileException("Class name cannot be empty");
		this.name = name;
		this.parent = parentScope;

		this.impl = impl;
	}

	public void addInterface(InterfaceScope iScope) throws CompileException {
        String symbolName = iScope.getName();
		
		if (interfaces.containsKey(iScope.getName())) throw new CompileException("Interface " + symbolName + " conflicts with interface of the same name");
		interfaces.put(symbolName, iScope);
	}

	public void addMethod(MethodSymbol method) throws CompileException {
		String symbolName = method.getName();
		if (interfaces.containsKey(symbolName)) throw new CompileException("Method name '" + symbolName + "' conflicts with interface name");
		
		String methodSignature = method.getSignature();
		for(MethodSymbol symbol : methods) {
			if(symbol.getName().equals(method.getName()) && methodSignature.equals(symbol.getSignature())) {
				throw new CompileException("Method '" + methodSignature + "' is already defined in interface '" + symbolName + "'");
			}
		}
		
		methods.add(method);
	}

	public Map<String, InterfaceScope> getInterfaces() {
		return interfaces;
	}

	public List<MethodSymbol> getMethods(String name) {
		List<MethodSymbol> result = new ArrayList<>();
		for(MethodSymbol mSymbol : methods) {
			if(mSymbol.getName().equals(name)) {
				result.add(mSymbol);
			}
		}
		return result;
	}
	
	public List<MethodSymbol> getMethods() {
		return methods;
	}

	@Override
	public Symbol resolveSymbol(String name) {
		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolveSymbol(name);
		}
		
		// Символ не найден
		return null;
	}
	
	public InterfaceScope resolveScope(String name) {
		// Поиск в импортах (если интерфейс объявлен в другом пакете)
		// Поиск в импортах (если интерфейс объявлен в другом пакете)
		String importedName = imports.get(name);
		if(null != importedName) {
			if(interfaces.containsKey(importedName)) return interfaces.get(importedName);
		}
		// Поиск в родительской области видимости (если есть)
		if(parent != null && parent instanceof InterfaceScope) {
			return ((InterfaceScope)parent).resolveScope(name);
		}
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

	public MethodSymbol resolveMethod(String methodName, VarType[] argTypes) throws CompileException {
		// Ищем методы в текущем классе
		for (MethodSymbol method : methods) {
			if (method.getName().equals(methodName) && isApplicable(method, argTypes)) {
				return method;
			}
		}
		return null;
	}

	protected boolean isApplicable(MethodSymbol method, VarType[] argTypes) throws CompileException {
		List<VarType> paramTypes = method.getParameterTypes();
		if (paramTypes.size() != argTypes.length) {
			return false;
		}

		for (int i=0; i<paramTypes.size(); i++) {
			// Нативный метод ничего не знет о Enum, но Enum может быть представлен как byte
			// Может быть предствален, но не стоит. Для вывода лучше использовать enum.index()
			// if(method.isNative && argTypes[i].isEnum() && VarType.BYTE==paramTypes.get(i)) continue;
			if (!AstNode.isCompatibleWith(this, paramTypes.get(i), argTypes[i])) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isImplements(VarType ifaceType) {
		if(null == impl) return false;
		if(ifaceType.isPrimitive()) return false;
		if(ifaceType.isObject()) return true;
		for(VarType type : impl) {
			if(ifaceType == type) return true;
			InterfaceScope iScope = resolveInterface(type.getClassName());
			if(iScope.isImplements(ifaceType)) return true;
		}
		return false;
	}
	
	public List<VarType> getImpl() {
		return impl;
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
	
	public int getMethodSN(String methodName, String signature) {
		for(int i=0; i<methods.size(); i++) {
			MethodSymbol mSymbol = methods.get(i);
			if(mSymbol.getName().equals(methodName) && mSymbol.getSignature().equals(signature))  return i;
		}
		return -1;
	}

	public List<AstNode> fillDepends(String signature) {
		List<AstNode> result = new ArrayList<>();
		
		Scope parentScope = parent;
		while(!(parentScope instanceof ClassScope)) {
			parentScope = parentScope.getParent();
		}
		ClassScope cScope = (ClassScope)parentScope;

		while(null != cScope) {
			for(ClassScope intcScope : cScope.getClasses()) {
				if(intcScope.isImplements(VarType.fromClassName(name))) {
					for(MethodSymbol mSymbol : intcScope.getMethods()) {
						if(mSymbol.getSignature().equals(signature)) {
							if(null != mSymbol.getNode()) {
								result.add(mSymbol.getNode());
							}
						}
					}
				}
			}

			cScope = (ClassScope)cScope.getParent();
		}
		return result;
	}
}