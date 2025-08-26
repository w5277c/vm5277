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

public class ClassScope extends InterfaceScope {
	private final	Map<String, String>				staticImports	= new HashMap<>();
	private	final	Map<String, Symbol>				fields			= new HashMap<>();
	private	final	Map<String, ClassScope>			classes			= new HashMap<>();
	private	final	List<MethodSymbol>				constructors	= new ArrayList<>();

	public ClassScope() throws CompileException {
		addInterface(new InterfaceScope("Object", this, null));
		MethodScope mScope = new MethodScope(null, this);
		MethodSymbol mSymbol = new MethodSymbol("getClassId", VarType.SHORT, null, true, false, false, false, mScope, null);
		mScope.setSymbol(mSymbol);
		addMethod(mSymbol);
		mScope = new MethodScope(null, this);
		mSymbol = new MethodSymbol("getClassTypeId", VarType.SHORT, null, true, false, false, false, mScope, null);
		mScope.setSymbol(mSymbol);
		addMethod(mSymbol);
	}
	
	public ClassScope(String name, Scope parentScope, List<VarType> impl) throws CompileException {
		this();
		
		if (null != parentScope && !(parentScope instanceof ClassScope)) throw new CompileException("Сlass " + name + " can only be declared within a class.");
		if (name == null || name.isEmpty()) throw new CompileException("Class name cannot be empty");

		this.parent = (ClassScope)parentScope;
		this.name = name;
		this.impl = impl;
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
		
/*		// Получаем символ Object из родительской области
		InterfaceScope objectInterface = resolveInterface("Object");
		if (null == objectInterface) throw new CompileException("Base interface 'Object' not found");
		if (!"Object".equals(scopeName)) {
			classScope.addInterface(objectInterface); // Добавляем существующий символ
		}
*/		
		classes.put(scopeName, classScope);
	}
	
	@Override
	public void addInterface(InterfaceScope iScope) throws CompileException {
        String symbolName = iScope.getName();
		
		if (imports.containsKey(name)) throw new CompileException("Interface '" + symbolName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new CompileException("Interface '" + symbolName + "' conflicts with static import");
		if (classes.containsKey(iScope.getName())) throw new CompileException("Interface " + symbolName + " conflicts with class of the same name");
		if (interfaces.containsKey(iScope.getName())) { throw new CompileException("Interface " + symbolName + " conflicts with interface of the same name");}
		interfaces.put(symbolName, iScope);
	}
	
	public void addConstructor(MethodSymbol newSymbol) throws CompileException {
		if(!name.equals(newSymbol.getName())) throw new CompileException("Constructor name must match class name '" + name + "'");
		
		String signature = newSymbol.getSignature();
		for(MethodSymbol symbol : constructors) {
			if(signature.equals(symbol.getSignature())) throw new CompileException("Duplicate constructor '" + signature + "'");
		}
		constructors.add(newSymbol);
	}
	
	@Override
	public void addMethod(MethodSymbol mSymbol) throws CompileException {
		String methodName = mSymbol.getName();
		
        if (imports.containsKey(name)) throw new CompileException("Method '" + methodName + "' conflicts with import");
        if (staticImports.containsKey(name)) throw new CompileException("Method '" + methodName + "' conflicts with static import");
		if (fields.containsKey(methodName)) throw new CompileException("Method name '" + methodName + "' conflicts with field name");
		if (classes.containsKey(methodName)) throw new CompileException("Method name '" + methodName + "' conflicts with class name");
		super.addMethod(mSymbol);
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
	
	public boolean checkStaticImportExists(String path) {
		return staticImports.containsKey(path);
	}
	
	public Map<String, Symbol> getFields() {
		return fields;
	}

	public List<MethodSymbol> getConstructors() {
		return constructors;
	}

	@Override
	public Symbol resolveSymbol(String name) {
		// Поиск в полях текущего класса
		if (fields.containsKey(name)) return fields.get(name);
		return super.resolveSymbol(name);
	}
	
	@Override
	public InterfaceScope resolveScope(String name) {
		// Поиск в текущем классе
		if(classes.containsKey(name)) return classes.get(name);
		if(interfaces.containsKey(name)) return interfaces.get(name);

		// Поиск в импортах (если интерфейс объявлен в другом пакете)
		String importedName = imports.get(name);
		if(null != importedName) {
			if(classes.containsKey(importedName)) return classes.get(importedName);
			if(interfaces.containsKey(importedName)) return interfaces.get(importedName);
		}
		// Поиск в родительской области видимости (если есть)
		if(parent != null && parent instanceof InterfaceScope) {
			return ((InterfaceScope)parent).resolveScope(name);
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
	
	private String getLastComponent(String path) {
		int pos = path.lastIndexOf('.');
		return pos >= 0 ? path.substring(pos+1) : path;
	}
	
	public List<ClassScope> getClasses() {
		return new ArrayList<>(classes.values());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}
}
