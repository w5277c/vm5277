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
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.DataSymbol;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGClassScope extends CGScope {
	private	final	VarType						type;
	private	final	VarType[]					interfaceTypes;
	private	final	Map<Integer, CGFieldScope>	fields			= new HashMap<>();
	private			int							filedsOffset	= 0;
	private	final	boolean						isImported;
	private			int							heapHeaderSize;
	private			CGLabelScope				lbIIDSScope;
	
	public CGClassScope(CodeGenerator cg, CGScope parent, int id, VarType type, VarType[] interfaceTypes, String name, boolean isRoot) {
		super(parent, id, name);
		
		this.type = type;
		this.interfaceTypes = interfaceTypes;
		this.isImported = isRoot;
		
		// адрес HEAP, счетчик ссылок, адрес блока реализаций класса и интерфейсов
		heapHeaderSize = 0x02 + 0x01 + 0x02;
		lbIIDSScope = new CGLabelScope(this, null, "I", true);
	}

	public void addField(CGFieldScope field) {
		fields.put(field.getResId(), field);
	}

	public CGCells memAllocate(int size, boolean isStatic) {
		if(isStatic) {
			CGCells cells = new CGCells(CGCells.Type.STAT, size, statOffset);
			statOffset+=size;
			return cells;
		}
		CGCells cells = new CGCells(CGCells.Type.HEAP, size, filedsOffset);
		filedsOffset+=size;
		return cells;
	}
	
	public void build(CodeGenerator cg) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) {
			cont.append(new CGIText(";build class " + getPath('.')));
		}

		// Формируем FLASH блок с ид типов класса и реализованных интерфейсов
		StringBuilder sb = new StringBuilder(lbIIDSScope.getName());
		sb.append(": .db ").append(0x01 + (null == interfaceTypes ? 0x00 : interfaceTypes.length)).append(",");
		sb.append(type.getId()).append(",");
		if(null != interfaceTypes) {
			for(VarType vt : interfaceTypes) {
				sb.append(vt.getId()).append(",");
			}
		}
		if(null != interfaceTypes && 0!=interfaceTypes.length%0x02) {
			sb.append("0");
		}
		else {
			sb.deleteCharAt(sb.length()-1);
		}
		cont.append(new CGIText(sb.toString()));
		prepend(cont);

		if(VERBOSE_LO <= verbose) append(new CGIText(";class end"));
	}
	
	public int getHeapOffset() {
		return filedsOffset;
	}
	
	public boolean isImported() {
		return isImported;
	}
	
	@Override
	public String getLName() {
		return "C" + name;
	}
	
	public VarType[] getInterfaceTypes() {
		return interfaceTypes;
	}
	
	public VarType getType() {
		return type;
	}
	
	//TODO предоставлять в CGFieldScope
	public int getHeapHeaderSize() {
		return heapHeaderSize;
	}
	
	public CGLabelScope getIIDLabel() {
		return lbIIDSScope;
	}
	
	@Override
	public String toString() {
		return "class " + name + " '" + getPath('.') + resId;
	}
}
