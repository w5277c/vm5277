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
import java.util.Collection;
import java.util.Collections;
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
import ru.vm5277.common.enums.OptimizationType;
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
	private	final	Set<Byte>					usedRegs			= new HashSet<>();	// Регистры, которые были задейстованы в коде метода - требуют сохранения оригинала в стек
	private	final	HashMap<Byte, RegPair>		regsPool;	// Свободные регистры, true = cвободен
	private			boolean						isUsed;
	private			CGIContainer				fieldsInitCallCont	= new CGIContainer("instance init caller");
	private			CGCells						cells;

	private			CGIContainer				initContainer		= new CGIContainer();
	
	private			boolean						heapIRegModified;
	private			AtomicInteger				lastHeapOffset		= new AtomicInteger();
	private			AtomicInteger				lastStackOffset		= new AtomicInteger();
	private			Set<Integer>				exceptionIds		= new HashSet<>();
	
	public CGMethodScope(CodeGenerator cg, CGClassScope parent, int resId, VarType type, int size, String name) {
		super(parent, resId, type, size, name);
		
		this.cg = cg;
		this.regsPool = cg.buildRegsPool();
	
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
		// Похоже полезно для любого метода - что за метод без точки входа?
		// Изначально задавалось только для persist методов в NewExpression.codeGen
		lbScope.setPersist();
	}
	
	public CGIContainer getInitContainer() {
		return initContainer;
	}
	
	public void build(CodeGenerator cg, CGExcs excs) throws CompileException {
		isUsed = true;
		
		CGIContainer cont = new CGIContainer();
		cont.append(new CGIText(""));
		cont.append(lbScope);

		heapIRegModified = cg.normalizeIRegConst(this, 'z', lastHeapOffset);
//FOR TEST heapIRegModified = true;
		cg.normalizeIRegConst(this, 'y', lastStackOffset);
		
		CGClassScope cScope = (CGClassScope)parent;
		if(null==type) {
			//cont.append(cg.eNewInstance(cScope.getHeapOffset(), cScope.getIIDLabel(), cScope.getType(), false, false));
			cont.append(initContainer);
			cont.append(fieldsInitCallCont);
			cont.append(lbCIScope);
//			fieldsInitCallCont = cg.call(null, cScope.getFieldInitLabel());
//			cont.append(fieldsInitCallCont);
		}

		prepend(cont);
		if(VERBOSE_LO <= verbose) prepend(new CGIText(";build " + toString()));
		
//		append(cg.eReturn(null, null == type ? 0x00 : type.getSize(), stackOffset));
		if(OptimizationType.FRONT.getId()<cg.getDevice().getOptimizationType().getId()) {
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
	public CGCells argAllocate(CGScope cgScope, int size) throws CompileException {
		CGCells cells = new CGCells(CGCells.Type.ARGS, size, stackOffset);
		stackOffset+=size;
		if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated args data, x" + size + " from stack frame (" + getPath('.') + ")"));
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
	public RegPair[] borrowReg(int size) throws CompileException {
		if(regsPool.isEmpty()) return null;

		if(0x02==size) {
			RegPair[] pairs = findRegsPair(regsPool);
			if(null!=pairs) {
				for(RegPair pair: pairs) {
					usedRegs.add(pair.getReg());	// Помечаем, что регистр использовался для сохраненния оригинала в стек
				}
				return pairs;
			}
		}
		else if(0x04==size) {
			RegPair[] pairs1 = findRegsPair(regsPool);
			if(null!=pairs1) {
				RegPair[] pairs2 = findRegsPair(regsPool);
				if(null!=pairs2) {
					for(RegPair pair: pairs1) {
						usedRegs.add(pair.getReg());	// Помечаем, что регистр использовался
					}
					for(RegPair pair: pairs2) {
						usedRegs.add(pair.getReg());	// Помечаем, что регистр использовался
					}
					return new RegPair[]{pairs1[0x00], pairs1[0x01], pairs2[0x00], pairs2[0x01]};
				}
				else {
					pairs1[0x00].setFree(true);
					pairs1[0x01].setFree(true);
				}
			}
		}
		
		RegPair[] result = new RegPair[size];
		List<RegPair> tmp = new ArrayList<>(regsPool.values());
		Collections.sort(tmp);
		int index = 0;
		for(RegPair pair : tmp) {
			if(pair.isFree()) {
				result[index++] = pair;
				if(index==size) {
					for(RegPair rp : result) {
						rp.setFree(false);
						usedRegs.add(rp.getReg());	// Помечаем, что регистр использовался
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
	
	protected void releaseReg(CGScope cgScope, Byte regId) throws CompileException {
		if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";release REG[" + regId + "] (" + getPath('.') + ") " + System.currentTimeMillis()));
		regsPool.get(regId).setFree(true);	// Помечаем, что регистр освобожден
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
	
	public Set<Byte> getUsedRegs() {
		return usedRegs;
	}
	
	public int getArgsStackSize() {
		return argsStackSize;
	}
	
	@Override
	public String toString() {
		return (null==type ? "constructor" : "method " + type) + " " + getPath('.') +  "(" + StrUtils.toString(types) + ")";
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
