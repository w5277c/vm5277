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

import ru.vm5277.common.VarType;

public class AliasSymbol extends Symbol {
	private	Symbol	symbol;
	
	public AliasSymbol(String name, VarType type, Symbol symbol) {
		super(name, type);
		this.symbol = symbol;
	}
	
	public Symbol getSymbol() {
		return symbol;
	}
	
	@Override
	public boolean isFinal() {
		return symbol.isFinal();
	}
	
	@Override
	public boolean isStatic() {
		return symbol.isStatic();
	}
	
	@Override
	public boolean isNative() {
		return symbol.isNative();
	}
	
	public void markUsed() {
		if(symbol instanceof VarSymbol) ((VarSymbol)symbol).markUsed();
	}
	public int getAccessCntr() {
		if(symbol instanceof VarSymbol) return ((VarSymbol)symbol).getAccessCntr();
		return -1;
	}
	
	@Override
	public String toString() {
		return	(isNative() ? "native " : "") + (isFinal() ? "final " : "") + (isStatic() ? "static " : "") + type + " " + name;
	}
}
