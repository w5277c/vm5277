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

import java.util.ArrayList;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGMethodScope extends CGBlockScope {
	private	final	CGLabelScope				lbScope;
	private	final	VarType						type;
	private	final	VarType[]					types;
	private	final	ArrayList<RegPair>			regsPool;	// Свободные регистры, true = cвободен
	
	public CGMethodScope(CodeGenerator cg, CGClassScope parent, int resId, VarType type, VarType[] types, String name, ArrayList<RegPair> regsPool) {
		super(parent, resId);
		
		this.type = type;
		this.types = types;
		this.name = name;
		this.regsPool = regsPool;
		
		lbScope = new CGLabelScope(this, null, null, true);
		cg.addLabel(lbScope);
	}
	
	@Override
	public void build(CodeGenerator cg) throws CompileException {
		build(cg, false);
	}
	public void build(CodeGenerator cg, boolean launchPoint) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";build " + toString()));
		if(launchPoint) cont.append(new CGIText("MAIN:"));
		cont.append(lbScope);
		prepend(cont);
		
		//super.build(cg);
	}

	//Выделение ячеек для переданных параметров (только стек)
	public CGCell[] paramAllocate(int size) throws CompileException {
		CGCell[] cells = new CGCell[size];
		for(int i=0; i<size; i++) {
			cells[i] = new CGCell(CGCell.Type.STACK, stackBlockOffset++);
		}
		return cells;
	}

	//TODO работает не корректно(не переиспользует регистры в вызванном методе)
	public RegPair borrowReg() {
		for(RegPair pair : regsPool) {
			if(pair.isFree()) {
				pair.setFree(false);
				return pair;
			}
		}
		return null;
	}
	public RegPair getReg(byte reg) {
		for(RegPair pair : regsPool) {
			if(pair.getReg() == reg) {
				return pair;
			}
		}
		return null;
	}

	@Override
	public boolean isFreeReg(byte reg) {
		RegPair regPair = getReg(reg);
		return null == regPair || regPair.isFree();
	}

	public CGLabelScope getLabel() {
		return lbScope;
	}
	
	@Override
	public String toString() {
		return "method " + type + " '" + getPath('.') +  "(" + StrUtils.toString(types) + "), id:" + resId;
	}
	
	@Override
	public String getLName() {
		return "M" + name;
	}
}
