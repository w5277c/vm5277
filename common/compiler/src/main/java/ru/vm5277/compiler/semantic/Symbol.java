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

import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.VarType;

//TODO содердание типа как в type, так и в Operand
public class Symbol {
	protected			String				name;
	protected			VarType				type;
	protected			boolean				isStatic;
	protected			boolean				isFinal;
	protected			boolean				isNative;
	private				CGScope				cgScope;
	private				boolean				reassigned = false;
	
	protected Symbol(String name) {
		this.name = name;
	}
	
	public Symbol(String name, VarType type) {
		this.name = name;
		this.type = type;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
	}

	public Symbol(String name, VarType type, boolean isFinal, boolean isStatic, boolean isNative) {
		this.name = name;
		this.type = type;
		this.isFinal = isFinal;
		this.isStatic = isStatic;
		this.isNative = isNative;
	}

	public String getName() {
		return name;
	}
	
	public VarType getType() {
		return type;
	}
	public void setType(VarType type) {
		this.type = type;
	}
	
	public void setCGScope(CGScope cgScope) {
		this.cgScope = cgScope;
	}
	public CGScope getCGScope() {
		return cgScope;
	}
	public<T> CGScope getCGScope(Class<T> clazz) {
		CGScope _scope = cgScope;
		while(null != _scope) {
			if(clazz.isInstance(_scope)) return _scope;
			_scope = _scope.getParent();
		}
		return null;
	}

	public boolean isFinal() {
		return isFinal;
	}
	public void setFinal(boolean isFinal) {
		this.isFinal = isFinal;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	public boolean isNative() {
		return isNative;
	}
	
	@Override
	public String toString() {
		return	(isNative ? "native " : "") + (isFinal ? "final " : "") + (isStatic ? "static " : "") + type + " " + name;
	}

	public void setReassigned() {
		reassigned = true;
	}
	public boolean isReassigned() {
		return reassigned;
	}
}