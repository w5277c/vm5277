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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.vm5277.common.cg.CGCell;

//TODO необходимо перевыделять блок в стеке, также нужно передавать размер выделенного блока как смещение для внутренних переменных
//TODO а в будущем, учитывать свободные регистры

public class CGBlockScope extends CGScope {
	protected	final	Map<Integer, CGLocalScope>	locals				= new HashMap<>();
	protected			int							stackBlockOffset	= 0;
	protected	final	List<Byte>					userRegs			= new ArrayList<>();

	public CGBlockScope(CGScope parent, int id, String name) {
		super(parent, id, name);
		
		CGBlockScope bScope = parent.getBlockScope();
		if(null != bScope) {
			stackBlockOffset = bScope.getStackBlockSize();
		}
	}
	
	public void addLocal(CGLocalScope local) {
		locals.put(local.getResId(), local);
	}
	public CGLocalScope getLocal(int resId) {
		CGLocalScope lScope = locals.get(resId);
		if(null != lScope) return lScope;
		
		if(null != parent && parent instanceof CGBlockScope) return ((CGBlockScope)parent).getLocal(resId);
		return null;
	}

	public int getStackBlockSize() {
		return stackBlockOffset;
	}

	public void putUsedReg(byte reg) {
		if(!userRegs.contains(reg)) {
			if(parent instanceof CGBlockScope) { // Если родитель тоже блок, и он не использует этот регистр, то выгодней сохранение регистра вынести в родитель
				CGBlockScope bScope =((CGBlockScope)parent);
				if(bScope.getUsedRegs().contains(reg)) {
					userRegs.add(reg);
				}
				else {
					bScope.putUsedReg(reg);
				}
			}
			else userRegs.add(reg);
		}
	}
	public void putUsedRegs(byte[] regs) {
		for(byte reg : regs) {
			putUsedReg(reg);
		}
	}
	public List<Byte> getUsedRegs() { // только для чтения!
		return userRegs;
	}

	public CGCell[] memAllocate(int size) {
		CGCell[] cells = new CGCell[size];
		for(int i=0; i<size; i++) {
			cells[i] = new CGCell(CGCell.Type.STACK, stackBlockOffset++);
		}
		return cells;
	}
}
