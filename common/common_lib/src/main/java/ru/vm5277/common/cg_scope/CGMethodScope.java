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
package ru.vm5277.common.cg_scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CGMethodScope extends CGScope {
	private	final	int							typeId;
	private	final	int[]						typeIds;
	private	final	Map<Integer, CGLocalScope>	locals		= new HashMap<>();
	private	final	List<Integer>				userRegs	= new ArrayList<>();
	
	public CGMethodScope(CGScope parent, int id, int typeId, int[] typeIds, String name) {
		super(parent, id, name);
		
		this.typeId = typeId;
		this.typeIds = typeIds;
	}
	
	public void addLocal(CGLocalScope local) {
		locals.put(local.getId(), local);
	}

	public int getStackSize() {
		int stackSize = 0;
		for(CGLocalScope lScope : locals.values()) {
			stackSize += lScope.getSize();
		}
		return stackSize;
	}
	
	public void putReg(int reg) {
		if(!userRegs.contains(reg)) userRegs.add(reg);
	}
	
	public List<Integer> getUsedRegs() { // только для чтения!
		return userRegs;
	}
}
