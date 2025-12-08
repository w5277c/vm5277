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

// Общий scope для Class и Interface
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.AstNode;

public abstract class CIScope extends Scope implements ImportableScope {
	protected			String						name;
	protected	final	Map<String, String>			staticImports	= new HashMap<>();
	protected	final	Map<String, String>			imports			= new HashMap<>();
	protected	final	Map<String, CIScope>		imported		= new HashMap<>();
	protected			List<VarType>				implTypes		= new ArrayList<>();	//extends и implements(наследование только интерфейсов)
	protected	final	Map<String, Symbol>			fields			= new HashMap<>();
	protected	final	List<MethodSymbol>			methods			= new ArrayList<>();
	
	public CIScope(Scope parent) {
		super(parent);
	}

	public void setImplTypes(List<VarType> implTypes) {
		this.implTypes = implTypes;
	}

	@Override
	public void addImport(String importPath, String alias) throws CompileException {
		String importName = (alias != null) ? alias : getLastComponent(importPath);
		if(imports.containsKey(importName)) {
			throw new CompileException("Duplicate import for name: " + importName);
		}
		imports.put(importName, importPath);
	}

	@Override
	public void addStaticImport(String importPath, String alias) throws CompileException {
		String importName = (alias!=null) ? alias : getLastComponent(importPath);
		if(staticImports.containsKey(importName)) {
			throw new CompileException("Duplicate static import for name: " + importName);
		}
		staticImports.put(importName, importPath);
	}

	@Override
	public boolean checkStaticImportExists(String path) {
		return staticImports.containsKey(path);
	}

	@Override
	public void addCI(CIScope cis, boolean isInternal) throws CompileException {
		if(name.equals(cis.getName())) {
			throw new CompileException(cis.getName() + " conflicts with class/interface/exception of the same name");
		}
		if(null!=imported.get(cis.getName())) throw new CompileException(cis.getName() + "' conflicts with import");

		if(isInternal) {
			addInternal(cis);
		}
		else {
			imported.put(cis.getName(), cis);
		}
	}
	
	public boolean isImplements(VarType ifaceType) {
		if(implTypes.isEmpty()) return false;
		if(ifaceType.isPrimitive()) return false;
		if(ifaceType.isObject()) return true;
		for(VarType type : implTypes) {
			if(ifaceType==type) return true;
			CIScope cis = resolveCI(type.getName(), false);
			if(null!=cis && cis instanceof InterfaceScope) {
				if(cis.isImplements(ifaceType)) return true;
			}
		}
		return false;
	}
	
	public List<VarType> getImpl() {
		return implTypes;
	}

	public void addField(Symbol symbol) throws CompileException {
		if(fields.containsKey(symbol.getName())) {
			throw new CompileException("Duplicate field: " + symbol.getName());
		}
		if(!symbol.isStatic() && this instanceof InterfaceScope) {
			throw new CompileException("Non static field '" + symbol.getName() + "' in interface '" + name + "'");
		}
		fields.put(symbol.getName(), symbol);
	}
	
	public Map<String, Symbol> getFields() {
		return fields;
	}
	
	public void addMethod(MethodSymbol method) throws CompileException {
		for(CIScope iScope : imported.values()) {
			iScope.resolveMethod(method.getName(), (VarType[])method.getParameterTypes().toArray(), false);
		}

		String methodSignature = method.getSignature();
		for(MethodSymbol symbol : methods) {
			if(symbol.getName().equals(method.getName()) && methodSignature.equals(symbol.getSignature())) {
				throw new CompileException("Method '" + methodSignature + "' is already defined in '" + name + "'");
			}
		}
		methods.add(method);
	}

	public int getMethodSN(String methodName, String signature) {
		for(int i=0; i<methods.size(); i++) {
			MethodSymbol mSymbol = methods.get(i);
			if(mSymbol.getName().equals(methodName) && mSymbol.getSignature().equals(signature)) return i;
		}
		return -1;
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

	public MethodSymbol resolveMethod(String methodName, VarType[] argTypes, boolean isQualifiedAccess) throws CompileException {
		// Ищем методы в текущем классе
		for (MethodSymbol method : methods) {
			if (method.getName().equals(methodName) && isApplicable(method, argTypes)) {
				return method;
			}
		}

		if(!isQualifiedAccess) {
			for(CIScope cis : internal.values()) {
				if(cis instanceof ClassScope) {
					MethodSymbol method = cis.resolveMethod(methodName, argTypes, false);
					if(null!=method) return method;
				}
			}
			for(CIScope cis : imported.values()) {
				if(cis instanceof ClassScope) {
					MethodSymbol method = cis.resolveMethod(methodName, argTypes, false);
					if(null!=method) return method;
				}
			}
			for(CIScope cis : internal.values()) {
				if(cis instanceof InterfaceScope) {
					MethodSymbol method = cis.resolveMethod(methodName, argTypes, false);
					if(null!=method) return method;
				}
			}
			for(CIScope cis : imported.values()) {
				if(cis instanceof InterfaceScope) {
					MethodSymbol method = cis.resolveMethod(methodName, argTypes, false);
					if(null!=method) return method;
				}
			}
		}
		return null;
	}

	@Override
	public CIScope resolveCI(Scope caller, String name, boolean isQualifiedAccess) {
		if(this.name.equals(name)) return this;
		
		if(isQualifiedAccess) {
			return internal.get(name);
		}

		CIScope cis = internal.get(name);
		if(null!=cis) return cis;
		
		//TODO здесь необходимо проверять доступ по модификаторам и caller
		cis = imported.get(name);
		if(null!=cis) return cis;
		
		if(null!=parent) {
			return parent.resolveCI(null==caller ? this : caller, name, false);
		}
		return null;
	}
	
	@Override
	public Symbol resolveField(Scope caller, String name, boolean isQualifiedAccess) throws CompileException {
		if(isQualifiedAccess) {
			// Поиск в полях только текущего класса
			return fields.get(name);
		}
		
		//TODO необходимо проверять доступ по модификаторам и caller
		
		// Поиск в полях текущего класса
		Symbol symbol = fields.get(name);
		if(null!=symbol) return symbol;
		
		for(VarType type : implTypes) {
			CIScope cis = resolveCI(null==caller ? this : caller, type.getName(), false);
			symbol = cis.resolveField(null==caller ? this : caller, name, false);
			if(null!=symbol) return symbol;
		}
		//TODO static inports
		// Делегируем в родительскую область видимости
		return parent!=null ? parent.resolveField(null==caller ? this : caller, name, false) : null;
	}
	
	@Override
	public Symbol resolveVar(String name) throws CompileException {
		return null;
	}
	
	protected boolean isApplicable(MethodSymbol method, VarType[] argTypes) throws CompileException {
		List<VarType> paramTypes = method.getParameterTypes();
		if(paramTypes.size()!=argTypes.length) {
			return false;
		}

		for(int i=0; i<paramTypes.size(); i++) {
			if(!AstNode.isCompatibleWith(this, paramTypes.get(i), argTypes[i])) {
				return false;
			}
		}
		return true;
	}
	
	public List<AstNode> fillDepends(String signature) {
		List<AstNode> result = new ArrayList<>();
		
		Scope parentScope = parent;
		while(!(parentScope instanceof ClassScope)) {
			parentScope = parentScope.getParent();
		}
		ClassScope cScope = (ClassScope)parentScope;

		while(null != cScope) {
			for(CIScope cis : cScope.getCIS()) {
				if(cis instanceof ClassScope && cis.isImplements(VarType.fromClassName(name))) {
					for(MethodSymbol mSymbol : cis.getMethods()) {
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

	private String getLastComponent(String path) {
		int pos = path.lastIndexOf('.');
		return pos >= 0 ? path.substring(pos+1) : path;
	}

	public List<MethodSymbol> getMethods() {
		return methods;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}
}
