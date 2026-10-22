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

import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.SourcePosition;

public class CGFieldScope extends CGCellsScope {
	private			boolean		isStatic;
	private			boolean		isConstant;
	private			CGCells		cells;
			
	public CGFieldScope(CGScope cgScope, SourcePosition sp, int resId, VarType type, int size, boolean isStatic, boolean isConstant, String name) {
		super(cgScope, sp, resId, type, size, name);
		
		this.isStatic = isStatic;
		this.isConstant = isConstant;
	}
	
	public void build() throws CompileException {
		if(isConstant) return;
		
		if(parent instanceof CGClassScope) {
			cells = ((CGClassScope)parent).memAllocate(size, isStatic);
		}
		else if(parent instanceof CGInterfaceScope && isStatic) {
			cells = ((CGInterfaceScope)parent).memAllocate(size);
		}
		else {
			throw new CompileException("Unexpected parent type for fields scope building:" + parent);
		}

		if(VERBOSE_LO <= verbose) prepend(new CGIText(";build " + toString() + ", allocated " + cells));
		if(parent instanceof CGClassScope) {
			((CGClassScope)parent).addField(this);
		}
	}
	
	@Override
	public CGCells getCells() {
		return cells;
	}
	public void setCells(CGCells cells) {
		this.cells = cells;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	@Override
	public String toString() {
		return "field " + type + " '" + getPath('.') + "'";
	}
}
