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

import java.util.List;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;

public class CGMethodScope extends CGBlockScope {
	private	final	CGLabelScope				lbScope;
	private	final	VarType						type;
	private	final	VarType[]					types;
	private	final	List<Byte>					regsPool;
	
	public CGMethodScope(CGClassScope parent, CGLabelScope lbScope, int resId, VarType type, VarType[] types, String name, List<Byte> regsPool) {
		super(parent, resId, name);
		
		this.lbScope = lbScope;
		this.type = type;
		this.types = types;
		this.regsPool = regsPool;
	}
	
	public void build(CodeGenerator cg) throws SemanticException {
		if(verbose) parent.append(new CGIText(";build " + toString()));
		
		if(userRegs.isEmpty()) {
			begin();
			for(int i=0; i<userRegs.size(); i++) {
				insert(cg.pushRegAsm(userRegs.get(i)));
			}
		}
		
		if(0x00 != stackBlockOffset) {
			cg.setDpStackAlloc();
			insert(cg.stackAllocAsm(stackBlockOffset));
		}
		
		if(userRegs.isEmpty()) {
			for(int i=userRegs.size()-1; i>=0; i--) {
				append(cg.popRegAsm(userRegs.get(i)));
			}
		}
		if(0x00 != stackBlockOffset) append(cg.stackFreeAsm());
	}
	
	
	public Byte reserveReg() {
		if(!regsPool.isEmpty()) {
			Byte reg = regsPool.remove(0x00);
			putUsedReg(reg);
			return reg;
		}
		return null;
	}
	public void releaseReg(List<Byte> regs) {
		for(Byte reg : regs) {
			if(null != reg) regsPool.add(reg);
		}
	}

	@Override
	public CGCell[] memAllocate(int size) {
		CGCell[] cells = new CGCell[size];
		for(int i=0; i<size; i++) {
			if(!regsPool.isEmpty()) {
				Byte reg = regsPool.remove(0x00);
				putUsedReg(reg);
				cells[i] = new CGCell(CGCell.Type.REG, reg);
			}
			else {
				cells[i] = new CGCell(CGCell.Type.STACK, stackBlockOffset++);
			}
		}
		return cells;
	}
	
	@Override
	public String toString() {
		return "method " + type + " '" + getPath(".") +  "(" + StrUtils.toString(types) + "), id:" + resId;
	}
}
