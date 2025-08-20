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
package ru.vm5277.common.cg.scopes;

import java.util.Arrays;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGVarScope extends CGCellsScope {
	private			boolean		isConstant;
	private			int			stackOffset;
	private			CGCells		cells;
			
	public CGVarScope(CGScope parent, int resId, VarType type, int size, boolean isConstant, String name) {
		super(parent, resId, type, size, name);
		
		this.isConstant = isConstant;
	}
	
	public void build() throws CompileException {
		if(!isConstant) {
			if(parent instanceof CGMethodScope) { // Используется при вызове метода(передача параметров)
				// Выделять память не нужно
				cells = ((CGMethodScope)parent).paramAllocate(size);
				if(VERBOSE_LO <= verbose) append(new CGIText(";build " + toString() + ", allocated " + cells));
				((CGMethodScope)parent).addArg(this);
			}
			else if(parent instanceof CGBlockScope) { // Инициализация переменной в блоке
				cells = ((CGBlockScope)parent).memAllocate(size);
				if(VERBOSE_LO <= verbose) append(new CGIText(";build " + toString() + ", allocated " + cells));
				((CGBlockScope)parent).addLocal(this);
			}
			else {
				throw new CompileException("TODO unexpected CGScope:" + parent + " for local init");
			}
		}
	}
	
	@Override
	public CGCells getCells() {
		return cells;
	}
	
	public void setStackOffset(int offset) {
		this.stackOffset = offset;
	}
	public int getStackOffset() {
		return stackOffset;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
	
	@Override
	public String toString() {
		return "var " + type + " '" + getPath('.') + "'";
	}
}
