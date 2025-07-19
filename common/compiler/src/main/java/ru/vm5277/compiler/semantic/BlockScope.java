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
import java.util.Map;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class BlockScope extends Scope {
	protected	final	Scope						parent;
	protected	final	Map<String, Symbol>			variables	= new HashMap<>();
	private		final	Map<String, LabelSymbol>	labels		= new HashMap<>();
	
	public BlockScope(Scope parent) {
		this.parent = parent;
	}

	public void addVariable(Symbol symbol) throws CompileException {
		String name = symbol.getName();
		if (variables.containsKey(name)) throw new CompileException("Duplicate variable: " + name);
		variables.put(name, symbol);
	}

	@Override
	public Symbol resolve(String name) {
		// 1. Ищем в локальных переменных
		Symbol symbol = variables.get(name);
		if (symbol != null) return symbol;

		// 2. Делегируем в родительскую область
		return parent != null ? parent.resolve(name) : null;
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
	public ClassScope getThis() {
		if(parent instanceof ClassScope) {
			return (ClassScope)parent;
		}
		return null == parent ? null : parent.getThis();
	}
	
	@Override
	public Scope getParent() {
		return parent;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
