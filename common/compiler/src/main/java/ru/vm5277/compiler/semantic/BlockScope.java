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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class BlockScope extends Scope {
	protected	final	Map<String, Symbol>			variables			= new HashMap<>();
	private		final	Map<String, LabelSymbol>	labels				= new HashMap<>();
	private		final	Set<ExceptionScope>			handlingExcsScopes	= new HashSet<>();
	
	public BlockScope(Scope parent) {
		super(parent);
	}

	public void addVariable(Symbol symbol) throws CompileException {
		String name = symbol.getName();
		if (variables.containsKey(name)) {
			throw new CompileException("Duplicate variable: " + name);
		}
		variables.put(name, symbol);
	}

	public void addHandlingExcsScope(ExceptionScope eScope) {
		handlingExcsScopes.add(eScope);
	}
	public Set<ExceptionScope> getHandlingExcsScopes() {
		return handlingExcsScopes;
	}
	
	@Override
	public CIScope resolveCI(Scope caller, String name, boolean isQualifiedAccess) {
		// Проверка модфификаторов доступа ненужна, все классы внутри блока доступны
		if(isQualifiedAccess) {
			return internal.get(name);
		}
		
		// Проверка модфификаторов доступа ненужна, все классы внутри блока доступны
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
			throw new CompileException("Qualified field access in block scope");
		}
		return parent.resolveField(null==caller ? this : caller, name, false);
	}
	
	@Override
	public Symbol resolveVar(String name) throws CompileException {
		// Ищем в локальных переменных
		Symbol symbol = variables.get(name);
		if(null!=symbol) return symbol;

		// 2. Делегируем в родительскую область(переменные для block и параметры для method)
		if(null!=parent) {
			return parent.resolveVar(name);
		}
		return null;
	}
	
	public void addLabel(LabelSymbol label) throws CompileException {
		String name = label.getName();
		if (labels.containsKey(name)) throw new CompileException("Duplicate label: " + name);
		labels.put(name, label);
	}

	public void addAlias(String name, VarType type, Symbol symbol) throws CompileException {
		Symbol oldSymbol = variables.get(name);
		if (null != oldSymbol && (!(oldSymbol instanceof AliasSymbol) || ((AliasSymbol)oldSymbol).getSymbol() != symbol)) {
			throw new CompileException("Duplicate variable: " + name);
		}
		variables.put(name, new AliasSymbol(name, type, symbol));
	}
	
	public LabelSymbol resolveLabel(String name) {
		// Ищем метку в текущей и родительских областях
		BlockScope current = this;
		while (current != null) {
			LabelSymbol symbol = current.labels.get(name);
			if(null != symbol) return symbol;
			current = (current.getParent() instanceof BlockScope) ? (BlockScope)current.getParent() : null;
		}
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
