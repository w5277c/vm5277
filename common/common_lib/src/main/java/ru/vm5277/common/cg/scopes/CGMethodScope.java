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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;

/*TODO 
	1.	В статических методах HeapIReg не задействован, можно использовать в пуле свободных регистров
	2.	Похоже в методе без переменных и без аргументов также не задействован stackIReg(нужно убедиться), можно использовать в пуле свободных регистров
		Но нужно убедтиться, что в блоках также нет локальных переменных.
	3.	Критично! AST inlining использует IReg для массивов, похоже нужно пересмотреть его зону применения как общую, без константного смещения(X регистр в AVR)
*/

public class CGMethodScope extends CGCellsScope {
	private			CodeGenerator				cg;
	private			CGLabelScope				lbScope;
	private			CGLabelScope				lbCIScope;
	private	final	List<CGBlockScope>			blockScopes			= new ArrayList<>();
	private	final	Map<Integer, CGVarScope>	args				= new HashMap<>();
	private			VarType[]					types;
	private			String						signature;
	private			int							argsStackSize;
	private			int							stackOffset			= 0;
	private	final	ArrayList<RegPair>			regsPool;	// Свободные регистры, true = cвободен
	private			boolean						isUsed;
	private			CGIContainer				fieldsInitCallCont;
	private			CGCells						cells;

	private			CGIContainer				initContainer		= new CGIContainer();
	
	private			boolean						heapIRegModified;
	private			AtomicInteger				lastHeapOffset		= new AtomicInteger();
	private			AtomicInteger				lastStackOffset		= new AtomicInteger();
	private			Set<Integer>				exceptionIds		= new HashSet<>();
	
	public CGMethodScope(CodeGenerator cg, CGClassScope parent, int resId, VarType type, int size, String name, ArrayList<RegPair> regsPool) {
		super(parent, resId, type, size, name);
		
		this.cg = cg;
		this.regsPool = regsPool;
	
		if(null!=type) {
			this.cells = new CGCells(CGCells.Type.ACC, size);
		}
		
	//	cg.addLabel(lbScope);
		
		if(null == type) {
			lbCIScope = new CGLabelScope(null, null, LabelNames.CONSTR_INIT, true);
	//		cg.addLabel(lbCIScope);
		}
	}
	
	public void setTypes(VarType[] types) {
		this.types = types;
		
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
		if(null != types) {
			for (VarType _type : types) {
				sb.append(_type.getName()).append(",");
				int typeSize = _type.getSize();
				argsStackSize += (-1 == typeSize ? cg.getRefSize() : typeSize);
			}
			if (0!=types.length) {
				sb.setLength(sb.length() - 1); // Удаляем последнюю запятую
			}
		}
		sb.append(")");
		signature = sb.toString();
		
		if((null == types || 0==types.length) && "main".equals(name) && "CMain".equals(parent.getLName())) {
			lbScope = new CGLabelScope(null, -1, LabelNames.MAIN, true);
		}
		else {
			lbScope = new CGLabelScope(this, null, null, true);
		}
	}
	
	public CGIContainer getInitContainer() {
		return initContainer;
	}
	
	public void build(CodeGenerator cg, CGExcs excs) throws CompileException {
		isUsed = true;
		
		CGIContainer cont = new CGIContainer();
		cont.append(new CGIText(""));
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";build " + toString()));
		cont.append(lbScope);

		heapIRegModified = cg.normalizeIRegConst(this, 'z', lastHeapOffset);
//FOR TEST heapIRegModified = true;
		cg.normalizeIRegConst(this, 'y', lastStackOffset);
		
		CGClassScope cScope = (CGClassScope)parent;
		if(null==type) {
			//cont.append(cg.eNewInstance(cScope.getHeapOffset(), cScope.getIIDLabel(), cScope.getType(), false, false));
			cont.append(initContainer);
			cont.append(lbCIScope);
//			fieldsInitCallCont = cg.call(null, cScope.getFieldInitLabel());
//			cont.append(fieldsInitCallCont);
		}
		prepend(cont);
		
//		append(cg.eReturn(null, null == type ? 0x00 : type.getSize(), stackOffset));
		if(Optimization.FRONT<cg.getPlatform().getOptLevel()) {
			CodeOptimizer co = cg.getOptimizer();
			if(null != co) {
//				co.removeUnusedLabels(this, lbScope, lbCIScope);
				co.optimizeAfterJump(this);
				co.optimizeRegLoad(parent);
			}
		}

		
		//append(cg.eReturn(null, stackOffset, -1, type));

		if(VERBOSE_LO <= verbose) append(new CGIText(";method end"));
	}
	
	//Выделение ячеек для переданных параметров (только стек)
	public CGCells argAllocate(int size) throws CompileException {
//		stackOffset+=size;
//		CGCells cells = new CGCells(CGCells.Type.STACK_FRAME, size, types.length-stackOffset);
		CGCells cells = new CGCells(CGCells.Type.ARGS, size, stackOffset);
		stackOffset+=size;
		return cells;
	}
	
	public void clearArgs() {
		args.clear();
		stackOffset=0;
	}
	public void addArg(CGVarScope local) {
		//TODO рудимент?
		args.put(local.getResId(), local);
	}
	public CGVarScope getArg(int resId) {
		CGVarScope lScope = args.get(resId);
		if(null!=lScope) return lScope;

		if(null!=parent) {
			CGScope scope = parent;
				
			if(scope instanceof CGMethodScope) return ((CGMethodScope)scope).getArg(resId);
			if(scope instanceof CGBlockScope) return ((CGBlockScope)scope).getVar(resId);
			if(scope instanceof CGCommandScope) return ((CGBlockScope)scope.getScope(CGBlockScope.class)).getVar(resId);
		}
		return null;
	}
	
	public void addExceptions(Set<Integer> ids) {
		exceptionIds.addAll(ids);
	}
	public boolean containsException(int id) {
		return exceptionIds.contains(id);
	}
	public Set<Integer> getExceptionIds() {
		return exceptionIds;
	}

	/**
	 * borrowReg занимаем регистры запрошенного количества, если регистров не достаточно возвращаем null
	 * @param size количество ячеек
	 * @return массив выделенных регистров либо null
	 */
	public RegPair[] borrowReg(int size) {
		if(regsPool.isEmpty()) return null;

		if(0x02==size) {
			RegPair[] pair = findPair();
			if(null!=pair) return pair;
		}
		else if(0x04==size) {
			RegPair[] pair1 = findPair();
			if(null!=pair1) {
				RegPair[] pair2 = findPair();
				if(null!=pair2) {
					return new RegPair[]{pair1[0x00], pair1[0x01], pair2[0x00], pair2[0x01]};
				}
				else {
					pair1[0x00].setFree(true);
					pair1[0x01].setFree(true);
				}
			}
		}
		
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
	// Вероятно эта парная аллокация полезна только для AVR. Похоже нужно будет вынести в библиотеку кодогенератора
	private RegPair[] findPair() {
		for(RegPair pair1 : regsPool) {
			if(pair1.isFree() && 0==(pair1.getReg()&0x01)) {
				for(RegPair pair2 : regsPool) {
					if(pair1.getReg()+0x01==pair2.getReg()) {
						pair1.setFree(false);
						pair2.setFree(false);
						return new RegPair[]{pair1, pair2};
					}
				}
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
	
	public boolean isFreeReg(byte reg) {
		RegPair regPair = getReg(reg);
		return null == regPair || regPair.isFree();
	}

	public CGLabelScope getLabel() {
		return lbScope;
	}
	public CGLabelScope getCILabel() {
		return lbCIScope;
	}
	
	public String getSignature() {
		return signature;
	}
	
	public int getArgStackSize() {
		return argsStackSize;
	}
	
	public VarType[] getTypes() {
		return types;
	}
	
	public boolean isUsed() {
		return isUsed;
	}
	
	public int getArgsStackSize() {
		return argsStackSize;
	}
	
	@Override
	public String toString() {
		return (null==type ? "constructor" : "method " + type) + " '" + getPath('.') +  "(" + StrUtils.toString(types) + ")";
	}
	
	@Override
	public String getLName() {
		return "M" + name;
	}

	public void addBlockScope(CGBlockScope bScope) {
		this.blockScopes.add(bScope);
	}
	public List<CGBlockScope> getBlockScopes() {
		return blockScopes;
	}
	
	public CGIContainer getFieldInitCallCont() {
		return fieldsInitCallCont;
	}

	@Override
	public CGCells getCells() {
		return cells;
	}
	
	public boolean isHeapIRegModified() {
		return heapIRegModified;
	}
	
	public int getLastHeapOffset() {
		return lastHeapOffset.get();
	}
	
	public int getLastStackOffset() {
		return lastStackOffset.get();
	}
}
