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
import java.util.Map;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;

// STACK(reg Y) - Необходимо выделять память в стеке для значений параметров, так-же сохранять старый Y и устанавливать новый на выделенный блок
// при завершении необходимо восстанавливать Y

public class CGMethodScope extends CGScope {
	private	final	CGLabelScope				lbScope;
	private	final	VarType						type;
	private	final	VarType[]					types;
	private	final	Map<Integer, CGVarScope>	args		= new HashMap<>();
	private			int							stackOffset	= 0;
	private			int							callSize;
	private	final	ArrayList<RegPair>			regsPool;	// Свободные регистры, true = cвободен
	
	public CGMethodScope(CodeGenerator cg, CGClassScope parent, int resId, VarType type, VarType[] types, String name, ArrayList<RegPair> regsPool) {
		super(parent, resId, name);
		
		this.type = type;
		this.types = types;
		this.regsPool = regsPool;
		this.callSize = cg.getCallSize();
		
		if((null == types || 0==types.length) && "main".equals(name) && "CMain".equals(parent.getLName())) {
			lbScope = new CGLabelScope(null, -1, "j8bCmainMmain", true);
		}
		else {
			lbScope = new CGLabelScope(this, null, null, true);
		}
		cg.addLabel(lbScope);
	}
	
	public void build(CodeGenerator cg) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";build " + toString()));
		cont.append(lbScope);
		prepend(cont);
		
		if(VERBOSE_LO <= verbose) append(new CGIText(";method end"));
	}

	//Выделение ячеек для переданных параметров (только стек)
	public CGCells paramAllocate(int size) throws CompileException {
		CGCells cells = new CGCells(CGCells.Type.STACK_FRAME, size, stackOffset);
		stackOffset+=size;
		return cells;
	}
	
	public void addArg(CGVarScope local) {
		args.put(local.getResId(), local);
	}
	public CGVarScope getArg(int resId) {
		return args.get(resId);
	}

	/**
	 * borrowReg занимаем регистры запрошенного количества, если регистров не достаточно возвращаем null
	 * @param size количество ячеек
	 * @return массив выделенных регистров либо null
	 */
	public RegPair[] borrowReg(int size) {
		if(regsPool.isEmpty()) return null;

		RegPair[] result = new RegPair[size];
		int index = 0;
		for(RegPair pair : regsPool) {
			if(pair.isFree()) {
				result[index++] = pair;
				if(index==size) {
					for(RegPair rp : result) {
						rp.setFree(false);
					}
					return result;
				}
			}
			else {
				index=0;
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

	public int getFiledsSize() {
		//TODO
		return 0;
	}
	
	public boolean isFreeReg(byte reg) {
		RegPair regPair = getReg(reg);
		return null == regPair || regPair.isFree();
	}

	public CGLabelScope getLabel() {
		return lbScope;
	}
	
	/**
	 * Возвращает размер стека для занятого праметрами + длина адреса возврата
	 * @return размер занятого стека
	 */
	public int getStackSize() {
		return stackOffset + callSize; 
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
