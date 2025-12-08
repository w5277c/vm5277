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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class ExceptionScope extends CIScope {
	private	final	static	String			RUNTIME_EXCEPTION_NAME	= "RuntimeException";
	private					int				id;
	private					ExceptionScope	extScope;
	private					List<String>	values;
	private					int				offset					= 0;
	
	public ExceptionScope(Scope parent, int id, String name, List<String> values) throws CompileException {
		super(parent);
		
		this.id = id;
		this.name = name;
		this.values = values;

		MethodScope mScope = new MethodScope(null, this);
		MethodSymbol ms = new MethodSymbol("throw", VarType.VOID, null, true, true, true, false, true, mScope, null);
		mScope.setSymbol(ms);
		addMethod(ms);
		
		mScope = new MethodScope(null, this);
		ms = new MethodSymbol("throw", VarType.VOID, Arrays.asList(new Symbol("code", VarType.BYTE)), true, true, true, false, true, mScope, null);
		mScope.setSymbol(ms);
		addMethod(ms);

	}
	
	//TODO аналогичная проверка нужна в InterfaceScope
	public void setExtScope(ExceptionScope eScope) throws CompileException {
		this.extScope = eScope;
		
		Set<ExceptionScope> visited = new HashSet<>();
		visited.add(this);
		
		if(this==eScope) {
			throw  new CompileException("Cyclic exception inheritance detected for:" + name);
		}

		ExceptionScope scope_ = eScope;
		while(null!=scope_) {
			if(!visited.add(scope_)) {
				throw new CompileException("Cyclic exception inheritance detected for:" + name);
			}
			scope_ = scope_.getExtScope();
		}
		
		offset = 0;
		scope_ = this.getExtScope();
		while(null!=scope_) {
			offset+=scope_.getValues().size();
			scope_ = scope_.getExtScope();
		}

		int total = offset+values.size();
		if(!values.isEmpty() && 0x100<total) {
			throw new CompileException("Exception constant index overflow: " + total + "(256 max) in " + name + " hierarchy");
		}
	}
	
	public boolean isUnchecked() {
		ExceptionScope scope_ = this;
		while(null!=scope_) {
			if(RUNTIME_EXCEPTION_NAME.equals(scope_.getName())) {
				return true;
			}
			scope_ = scope_.getExtScope();
		}
		return false;
	}

	public boolean isCompatible(Set<ExceptionScope> eScopes) {
		for(ExceptionScope eScope : eScopes) {
			if(this==eScope) {
				return true;
			}

			ExceptionScope eScope_ = this;
			while(null!=eScope_) {
				if(eScope==eScope_) {
					return true;
				}
				eScope_ = eScope_.getExtScope();
			}
		}
		return false;
	}

	public boolean isCompatible(int exceptionId) {
		if(id==exceptionId) {
			return true;
		}

		ExceptionScope eScope_ = this;
		while(null!=eScope_) {
			if(exceptionId==eScope_.getId()) {
				return true;
			}
			eScope_ = eScope_.getExtScope();
		}
		return false;
	}

	public int getId() {
		return id;
	}
	
	public ExceptionScope getExtScope() {
		return extScope;
	}
	
	public int getValueIndex(String value) throws CompileException {
		int index = values.indexOf(value);
		if(-1==index) {
			if(null!=extScope) {
				return extScope.getValueIndex(value);
			}
			throw new CompileException("COMPILER ERROR: Excpetion value '" + value + "' not found in exception '" + name);
		}
		
		return offset+index;
	}

	public List<String> getValues() {
		return values;
	}
	
	public String getValue(int index) {
		if(values.size()<=index) return null;
		return values.get(index);
	}
	
	public int getSize() {
		return values.size();
	}

	@Override
	public CIScope resolveCI(Scope caller, String name, boolean isQualifiedAccess) {
		return null;
	}

	@Override
	public Symbol resolveField(Scope caller, String name, boolean isQualifiedAccess) throws CompileException {
		getValueIndex(name);
		
		return new FieldSymbol(name, VarType.BYTE, true, true, false, this, null);
	}

	@Override
	public Symbol resolveVar(String name) throws CompileException {
		return null;
	}
}
