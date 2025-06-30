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
import ru.vm5277.common.cg.CGCell;

public class CGMethodScope extends CGBlockScope {
	private	final	CGLabelScope				lbScope;
	private	final	int							typeId;
	private	final	int[]						typeIds;
	private	final	List<Byte>					regsPool;
	
	public CGMethodScope(CGScope parent, CGLabelScope lbScope, int id, int typeId, int[] typeIds, String name, List<Byte> regsPool) {
		super(parent, id, name);
		
		this.lbScope = lbScope;
		this.typeId = typeId;
		this.typeIds = typeIds;
		this.regsPool = regsPool;
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
}
