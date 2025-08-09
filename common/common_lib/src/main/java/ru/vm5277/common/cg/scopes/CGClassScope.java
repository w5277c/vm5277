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

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGClassScope extends CGScope {
	private	final	VarType						type;
	private	final	int[]						intrerfaceIds;
	private	final	Map<Integer, CGFieldScope>	fields			= new HashMap<>();
	private			int							heapOffset		= 0;
	private	final	boolean						isImported;
	
	public CGClassScope(CGScope parent, int id, VarType type, int[] intrerfaceIds, String name, boolean isRoot) {
		super(parent, id, name);
		
		this.type = type;
		this.intrerfaceIds = intrerfaceIds;
		this.isImported = isRoot;
	}

	public void addField(CGFieldScope field) {
		fields.put(field.getResId(), field);
	}

	public CGCell[] memAllocate(int size, boolean isStatic) {
		CGCell[] cells = new CGCell[size];
		for(int i=0; i<size; i++) {
			if(isStatic) {
				cells[i] = new CGCell(CGCell.Type.STAT, statOffset++);
			}
			else {
				cells[i] = new CGCell(CGCell.Type.HEAP, heapOffset++);
			}
		}
		return cells;
	}
	
	public void build(CodeGenerator cg) throws CompileException {
		if(VERBOSE_LO <= verbose) prepend(new CGIText(";build class " + getPath('.') + ",id:" + resId));
		if(VERBOSE_LO <= verbose) append(new CGIText(";class end"));
	}
	
	public int getHeapOffset() {
		return heapOffset;
	}
	
	public boolean isImported() {
		return isImported;
	}
	
	@Override
	public String getLName() {
		return "C" + name;
	}
	
	@Override
	public String toString() {
		return "class " + name + " '" + getPath('.') + resId;
	}
}
