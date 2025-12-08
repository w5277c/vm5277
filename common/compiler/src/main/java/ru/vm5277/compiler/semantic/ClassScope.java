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
import java.util.List;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class ClassScope extends CIScope {
	private	final	List<MethodSymbol>				constructors	= new ArrayList<>();
	
	public ClassScope(Scope parent, String name) throws CompileException {
		super(parent);
		
		addInternal(new InterfaceScope("Object", this, new ArrayList<>()));
		MethodScope mScope = new MethodScope(null, this);
		MethodSymbol mSymbol = new MethodSymbol("getClassId", VarType.SHORT, null, true, false, false, false, true, mScope, null);
		mScope.setSymbol(mSymbol);
		methods.add(mSymbol);
		mScope = new MethodScope(null, this);
		mSymbol = new MethodSymbol("getClassTypeId", VarType.SHORT, null, true, false, false, false, true, mScope, null);
		mScope.setSymbol(mSymbol);
		methods.add(mSymbol);
		
		if(null!=parent && !(parent instanceof GlobalScope) && !(parent instanceof ClassScope)) {
			throw new CompileException("Сlass " + name + " can only be declared within a class.");
		}
		if(name==null || name.isEmpty()) {
			throw new CompileException("Class name cannot be empty");
		}

		this.name = name;
	}
	
	public void addConstructor(MethodSymbol newSymbol) throws CompileException {
		if(!name.equals(newSymbol.getName())) {
			throw new CompileException("Constructor name must match class name '" + name + "'");
		}
		
		String signature = newSymbol.getSignature();
		for(MethodSymbol symbol : constructors) {
			if(signature.equals(symbol.getSignature())) {
				throw new CompileException("Duplicate constructor '" + signature + "'");
			}
		}
		constructors.add(newSymbol);
	}
	
	public List<MethodSymbol> getConstructors() {
		return constructors;
	}

	public MethodSymbol resolveConstructor(String methodName, VarType[] argTypes, boolean isQualifiedAccess) throws CompileException {
		// Ищем методы в текущем классе
		for (MethodSymbol method : constructors) {
			if (method.getName().equals(methodName) && isApplicable(method, argTypes)) {
				return method;
			}
		}

		if(!isQualifiedAccess) {
			for(CIScope cis : internal.values()) {
				if(cis instanceof ClassScope) {
					MethodSymbol method = ((ClassScope)cis).resolveConstructor(methodName, argTypes, false);
					if(null!=method) return method;
				}
			}
			for(CIScope cis : imported.values()) {
				if(cis instanceof ClassScope) {
					MethodSymbol method = ((ClassScope)cis).resolveMethod(methodName, argTypes, false);
					if(null!=method) return method;
				}
			}
		}
		return null;
	}
}
