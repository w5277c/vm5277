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

public class ClassScope extends Scope {
	private			ClassScope						parent;
	private			String							name;
	
	private final	Map<String, String>				imports			= new HashMap<>();
	private final	Map<String, String>				staticImports	= new HashMap<>();
	private	final	Map<String, Symbol>				fields			= new HashMap<>();
	private	final	Map<String, ClassScope>			classes			= new HashMap<>();
	private	final	Map<String, InterfaceSymbol>	interfaces		= new HashMap<>();
	private	final	Map<String, List<MethodSymbol>>	methods			= new HashMap<>();
	private	final	List<MethodSymbol>				constructors	= new ArrayList<>();

	public ClassScope() {
	}
	
	public ClassScope(String name, Scope parentScope) throws CompileException {
		if (null != parentScope && !(parentScope instanceof ClassScope)) throw new CompileException("Сlass " + name + " can only be declared within a class.");
		if (name == null || name.isEmpty()) throw new CompileException("Class name cannot be empty");

		this.parent = (ClassScope)parentScope;
		this.name = name;
	}
	
	public void addImport(String importPath, String alias) throws CompileException {
		String importName = (alias != null) ? alias : getLastComponent(importPath);
		if (imports.containsKey(importName)) throw new CompileException("Duplicate import for name: " + importName);
		imports.put(importName, importPath);
	}

	public void addStaticImport(String importPath, String alias) throws CompileException {
		String importName = (alias != null) ? alias : getLastComponent(importPath);
		if (staticImports.containsKey(importName)) throw new CompileException("Duplicate static import for name: " + importName);
		staticImports.put(importName, importPath);
	}
	
	public void addClass(ClassScope classScope) throws CompileException {
        String scopeName = classScope.getName();
		
		if (imports.containsKey(name)) throw new CompileException("Class '" + scopeName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new CompileException("Class '" + scopeName + "' conflicts with static import");
		if (classes.containsKey(classScope.getName())) throw new CompileException("Duplicate class: " + scopeName);
		if (interfaces.containsKey(classScope.getName())) throw new CompileException("Class: " + scopeName + " already defined as interface");
		
		// Получаем символ Object из родительской области
		InterfaceSymbol objectInterface = resolveInterface("Object");
		if (null == objectInterface) throw new CompileException("Base interface 'Object' not found");
		if (!"Object".equals(scopeName)) {
			classScope.addInterface(objectInterface); // Добавляем существующий символ
		}
		
		classes.put(scopeName, classScope);
	}
	
	public void addInterface(InterfaceSymbol symbol) throws CompileException {
        String symbolName = symbol.getName();
		
		if (imports.containsKey(name)) throw new CompileException("Interface '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new CompileException("Interface '" + symbolName + "' conflicts with static import");
		if (classes.containsKey(symbol.getName())) throw new CompileException("Interface " + symbolName + " conflicts with class of the same name");
		if (interfaces.containsKey(symbol.getName())) throw new CompileException("Class " + symbolName + " conflicts with interface of the same name");
		interfaces.put(symbolName, symbol);
	}
	
	public void addConstructor(MethodSymbol newSymbol) throws CompileException {
		if(!name.equals(newSymbol.getName())) throw new CompileException("Constructor name must match class name '" + name + "'");
		
		String signature = newSymbol.getSignature();
		for(MethodSymbol symbol : constructors) {
			if(signature.equals(symbol.getSignature())) throw new CompileException("Duplicate constructor '" + signature + "'");
		}
		constructors.add(newSymbol);
	}
	
	public void addMethod(MethodSymbol newSymbol) throws CompileException {
		String symbolName = newSymbol.getName();
		
        if (imports.containsKey(name)) throw new CompileException("Method '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new CompileException("Method '" + symbolName + "' conflicts with static import");
		if (fields.containsKey(symbolName)) throw new CompileException("Method name '" + symbolName + "' conflicts with field name");
		if (classes.containsKey(symbolName)) throw new CompileException("Method name '" + symbolName + "' conflicts with class name");
		if (interfaces.containsKey(symbolName)) throw new CompileException("Method name '" + symbolName + "' conflicts with interface name");

		
		// Получаем список методов с таким же именем
		List<MethodSymbol> methodsWithSameName = methods.get(symbolName);
		if (null == methodsWithSameName) {
			methodsWithSameName = new ArrayList<>();
			methods.put(symbolName, methodsWithSameName);
		}
		
		// Проверяем на перегрузку/дублирование
		String newSignature = newSymbol.getSignature();
		for (MethodSymbol existingMethod : methodsWithSameName) {
			// Проверяем полное совпадение сигнатур
			if (newSignature.equals(existingMethod.getSignature())) throw new CompileException("Duplicate method '" + newSignature + "' in class '" +
																								name + "'");
			// Дополнительная проверка на конфликт при наследовании (если нужно)
			if (newSymbol.getType().equals(existingMethod.getType()) &&
				newSymbol.getParameterTypes().equals(existingMethod.getParameterTypes())) throw new CompileException(	"Method '" + newSignature +
																														"' conflicts with inherited method");
		}

		methodsWithSameName.add(newSymbol);
	}

	public void addField(Symbol symbol) throws CompileException {
		String symbolName = symbol.getName();
		
        if (imports.containsKey(symbolName)) throw new CompileException("Field '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(symbolName)) throw new CompileException("Field '" + symbolName + "' conflicts with static import");
		if (classes.containsKey(symbolName)) throw new CompileException("Field name " + symbolName + " conflicts with class name");
		if (interfaces.containsKey(symbolName)) throw new CompileException("Field name " + symbolName + " conflicts with interface name");
		if (fields.containsKey(symbolName)) throw new CompileException("Duplicate field: " + symbolName);
		fields.put(symbolName, symbol);
	}
	
	@Override
	public ClassScope getThis() {
		return this;
	}
	
	public Map<String, InterfaceSymbol> getInterfaces() {
		return interfaces;
	}
	
	public boolean checkStaticImportExists(String path) {
		return staticImports.containsKey(path);
	}
	
	public List<MethodSymbol> getMethods(String methodName) {
		return methods.get(methodName);
	}
	
	public Map<String, List<MethodSymbol>> getMethods() {
		return methods;
	}
	
	public Map<String, Symbol> getFields() {
		return fields;
	}

	public List<MethodSymbol> getConstructors() {
		return constructors;
	}


	@Override
	public Symbol resolve(String name) {
		// Поиск в полях текущего класса
		if (fields.containsKey(name)) {
			return fields.get(name);
		}

		// Поиск методов (без параметров - для простых случаев)
		if (methods.containsKey(name) && !methods.get(name).isEmpty()) {
			// Возвращаем первый метод с таким именем (для точного поиска нужно использовать resolveMethod)
			return methods.get(name).get(0);
		}

		// Поиск в интерфейсах
		if (interfaces.containsKey(name)) {
			return interfaces.get(name);
		}

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolve(name);
		}

		// Символ не найден
		return null;
	}
	
	public ClassScope resolveClass(String className) {
		// Поиск в текущем классе
		if (classes.containsKey(className)) return classes.get(className);

		// Поиск в импортах (если интерфейс объявлен в другом пакете)
		String importedName = imports.get(className);
		if (null != importedName && interfaces.containsKey(importedName)) return classes.get(importedName);

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolveClass(className);
		}
		
		return null;
	}

	public InterfaceSymbol resolveInterface(String interfaceName) {
		// Поиск в текущем классе
		if (interfaces.containsKey(interfaceName)) return interfaces.get(interfaceName);

		// Поиск в импортах (если интерфейс объявлен в другом пакете)
		String importedName = imports.get(interfaceName);
		if (null != importedName && interfaces.containsKey(importedName)) return interfaces.get(importedName);

		// Поиск в родительской области видимости (если есть)
		if (parent != null) {
			return parent.resolveInterface(interfaceName);
		}

		return null;
	}
	
	public MethodSymbol resolveMethod(String methodName, VarType[] argTypes) throws CompileException {
		// Ищем методы в текущем классе
		List<MethodSymbol> candidates = methods.get(methodName);
		if (null == candidates) return null;
		for (MethodSymbol method : candidates) {
			if (isApplicable(method, argTypes)) {
				return method;
			}
		}
		return null;
	}

	public MethodSymbol resolveConstructor(String methodName, VarType[] argTypes) throws CompileException {
		// Ищем конструкторы в текущем классе
		for (MethodSymbol method : constructors) {
			if (isApplicable(method, argTypes)) {
				return method;
			}
		}
		return null;
	}
	/*TODO	
	public MethodSymbol resolveStaticImport(String methodName, List<VarType> argTypes) {
		for (MethodSymbol method : staticImports.values()) {
			if (method.getName().equals(methodName) && isArgumentsMatch(method, argTypes)) return method;
		}
		return null;
	}
*/
	
	private boolean isApplicable(MethodSymbol method, VarType[] argTypes) throws CompileException {
		List<VarType> paramTypes = method.getParameterTypes();
		if (paramTypes.size() != argTypes.length) {
			return false;
		}

		for (int i=0; i<paramTypes.size(); i++) {
			if (!AstNode.isCompatibleWith(this, paramTypes.get(i), argTypes[i])) {
				return false;
			}
		}
		return true;
	}
	
	private String getLastComponent(String path) {
		int pos = path.lastIndexOf('.');
		return pos >= 0 ? path.substring(pos+1) : path;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public Scope getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}
}
