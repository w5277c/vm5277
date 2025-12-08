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

import java.util.HashSet;
import java.util.Set;
import ru.vm5277.common.exceptions.CompileException;

public class MethodScope extends Scope {
	private	MethodSymbol		mSymbol;
	private	Set<ExceptionScope>	exceptionScopes		= new HashSet<>();	// исключения заданные через throws
	
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

	public void addExceptionScope(ExceptionScope eScope) {
		exceptionScopes.add(eScope);
	}
	public boolean containsException(ExceptionScope eScope) {
		return exceptionScopes.contains(eScope);
	}
	public Set<ExceptionScope> getExceptionScopes() {
		return exceptionScopes;
	}
	
	@Override
	public CIScope resolveCI(Scope caller, String name, boolean isQualifiedAccess) {
		if(isQualifiedAccess) {
			return internal.get(name);
		}
		
		CIScope cis = internal.get(name);
		if(null!=cis) return cis;
		
		if(null!=parent) {
			return parent.resolveCI(null==caller ? this : caller, name, false);
		}
		return null;
	}

	@Override
	public Symbol resolveField(Scope caller, String name, boolean isQualifiedAccess) throws CompileException {
		if(isQualifiedAccess) {
			throw new CompileException("COMPILER BUG: Qualified field access in method scope");
		}
		return parent.resolveField(null==caller ? this : caller, name, false);
	}
	
	@Override
	public Symbol resolveVar(String name) throws CompileException {
		// Проверяем параметры метода
		for(Symbol param : mSymbol.getParameters()) {
			if(param.getName().equals(name)) {
				return param;
			}
		}
		return null;
	}
	
	@Override
	public String toString() {
		return mSymbol.toString();
	}

	@Override
	public Scope getParent() {
		return parent;
	}
}