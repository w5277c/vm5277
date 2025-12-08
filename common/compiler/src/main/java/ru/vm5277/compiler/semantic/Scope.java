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

public abstract class Scope {
	protected			Scope						parent;
	protected	final	Map<String, CIScope>		internal	= new HashMap<>();

	public Scope(Scope parent) {
		this.parent = parent;
	}
	
	public void addInternal(CIScope cis) throws CompileException {
		if(null!=internal.get(cis.getName())) {
			throw new CompileException(cis.getName() + " conflicts with class/interface/exception of the same name");
		}

		internal.put(cis.getName(), cis);
	}
	
	public List<CIScope> getCIS() {
		return new ArrayList<>(internal.values());
	}

	public abstract CIScope resolveCI(Scope caller, String name, boolean isQualifiedAccess);	
	public CIScope resolveCI(String name, boolean isQualifiedAccess) {
		return resolveCI(null, name, isQualifiedAccess);
	}
	public abstract Symbol resolveField(Scope caller, String name, boolean isQualifiedAccess) throws CompileException;
	public Symbol resolveField(String name, boolean isQualifiedAccess) throws CompileException {
		return resolveField(null, name, isQualifiedAccess);
	}
	public abstract Symbol resolveVar(String name) throws CompileException;

	public Scope getParent() {
		return parent;
	}

	public CIScope getThis() {
		Scope _scope = this;
		while(null!=_scope) {
			if(_scope instanceof CIScope) {
				return (CIScope)_scope;
			}
			_scope = _scope.getParent();
		}
		return null;
	}
	
	public MethodScope getMethod() {
		Scope _scope = this;
		while(null!=_scope) {
			if(_scope instanceof MethodScope) {
				return (MethodScope)_scope;
			}
			_scope = _scope.getParent();
		}
		return null;
	}
}