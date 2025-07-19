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
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

public class CGMethodScope extends CGBlockScope {
	private	final	CGLabelScope				lbScope;
	private	final	VarType						type;
	private	final	VarType[]					types;
	private	final	List<Byte>					regsPool;
	private			int							labelIdCntr	= 1;
	
	public CGMethodScope(CodeGenerator cg, CGClassScope parent, int resId, VarType type, VarType[] types, String name, List<Byte> regsPool) {
		super(parent, resId, name);
		
		this.type = type;
		this.types = types;
		this.regsPool = regsPool;

		lbScope = new CGLabelScope(this, labelIdCntr++, null, true);
		cg.addLabel(lbScope);
	}
	
	public void build(CodeGenerator cg, boolean launchPoint) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(verbose) cont.append(new CGIText(";build " + toString()));
		if(launchPoint) cont.append(new CGIText("MAIN:"));
		cont.append(lbScope);
		
		
		if(userRegs.isEmpty()) {
			for(int i=0; i<userRegs.size(); i++) {
				cont.append(cg.pushRegAsm(userRegs.get(i)));
			}
		}
		
		if(0x00 != stackBlockOffset) {
			cg.setDpStackAlloc();
			cont.append(cg.stackAllocAsm(stackBlockOffset));
		}
		prepend(cont);
		
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

	
	//cells(null != oldCells - генерация локальных переменных при вызове не нативного метода):
	//регистр - не меняем, просто push/pop(все регистры проверяем в первую очередь)
	//стек - копируем в новые cells
	//куча - копируем в новые cells
	//остальное просто используем старые cells

	public CGCell[] memAllocate(int size, CGCell[] oldCells) throws CompileException {
		CGCell[] cells = new CGCell[size];
		for(int i=0; i<size; i++) {
			if(null != oldCells) {
				CGCell oldCell = oldCells[i];
				if(CGCell.Type.REG == oldCell.getType()) {
					byte reg = (byte)oldCell.getNum();
					int index = regsPool.indexOf(reg);
					if(-1 != index) {
						regsPool.remove(index);
						putUsedReg((byte)reg);
						cells[i] = new CGCell(CGCell.Type.REG, (byte)reg);
					}
					else throw new CompileException("Register already used for allocate");
				}
				else if(CGCell.Type.STACK == oldCell.getType()) {
					cells[i] = new CGCell(CGCell.Type.STACK, stackBlockOffset++);
				}
				else {
					throw new CompileException("Unexpected cell type " + oldCell.getType() + " for allocate");
				}
			}
			else {
				if(!regsPool.isEmpty()) {
					Byte reg = regsPool.remove(0x00);
					putUsedReg(reg);
					cells[i] = new CGCell(CGCell.Type.REG, reg);
				}
				else {
					cells[i] = new CGCell(CGCell.Type.STACK, stackBlockOffset++);
				}
			}
		}
		return cells;
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
		return "m" + name.toUpperCase();
	}
}
