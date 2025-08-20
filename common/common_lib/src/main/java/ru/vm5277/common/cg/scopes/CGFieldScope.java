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
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGFieldScope extends CGCellsScope {
	private			boolean		isStatic;
	private			CGCells		cells;
			
	public CGFieldScope(CGClassScope cScope, int resId, VarType type, int size, boolean isStatic, String name) {
		super(cScope, resId, type, size, name);
		
		this.isStatic = isStatic;
	}
	
	public void build() throws CompileException {
		cells = ((CGClassScope)parent).memAllocate(size, isStatic);
		if(VERBOSE_LO <= verbose) append(new CGIText(";build " + toString() + ", allocated " + cells));
		((CGClassScope)parent).addField(this);
	}
	
	@Override
	public CGCells getCells() {
		return cells;
	}
	
	public boolean isStatic() {
		return isStatic;
	}
	
	@Override
	public String toString() {
		return "field " + type + " '" + getPath('.') + "'";
	}
}
