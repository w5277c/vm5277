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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import static ru.vm5277.common.cg.scopes.CGScope.verbose;
import ru.vm5277.common.enums.InstanceType;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.exceptions.CompileException;

// STACK(reg Y) - необходимо выделять блок памяти в стеке для локальных переменных, Y не изменяем. Y изменяется только если выходим за 64 слова, затем
// его нужно восстановить. Смещения адресов локальных перменных высчитывааются от начала выделенного блока в методе


public class CGBlockScope extends CGScope {
	private				CGLabelScope				lbEScope;
	private		final	Map<Integer, CGVarScope>	locals			= new HashMap<>();
	// Хранит размер блока в стеке, выделенный под локальные переменные
	protected			int							stackOffset		= 0;				
	// Все доступные регистры под переменные, хранят признак свободен(true)/занят для текущего уровня
	private		final	HashMap<Byte,RegPair>		regsPool;
	// Регистры, которые одолжили у родителя (в конце должны сообщить родителю весь список для освобождения)
	private				Set<Byte>					parentUsedRegs	= new HashSet<>();	
	// Регистры, которые были задействованы в коде блока и требуют сохранения оригинала в стек
	private		final	Set<Byte>					localUsedRegs	= new HashSet<>();
	private				CGMethodScope				mScope;
	private				boolean						isFirstBlock;
	private				Set<String>					labels			= new HashSet<>();
	
	public CGBlockScope(CodeGenerator cg, CGScope parent, int id, String comment) {
		super(parent, id, comment);
		
/*		CGBlockScope bScope = parent.getBlockScope();
		if(null != bScope) {
			this.stackOffset = bScope.getStackBlockSize();
		}
*/		
		regsPool = cg.buildRegsPool();

		if(parent instanceof CGMethodScope) isFirstBlock = true;

		mScope = (CGMethodScope)parent.getScope(CGMethodScope.class);
		mScope.addBlockScope(this);

		lbEScope = new CGLabelScope(null, null, LabelNames.BLOCK_END, false);
//		cg.addLabel(lbEScope);
	}
	
	public void build(CodeGenerator cg, boolean isLaunchPoint, CGExcs excs) throws CompileException {
		CGIContainer cont = new CGIContainer();

		if(0!=mScope.getArgsStackSize() || 0!=stackOffset) {
			cg.stackPrepare(cont, isFirstBlock, mScope.getArgsStackSize(), stackOffset, excs);
		}

		List<Byte> regs = new ArrayList<>(getMethodScope().getUsedRegs());
		if(!isLaunchPoint && isFirstBlock) {
			if(!regs.isEmpty()) {
				Collections.sort(regs, new Comparator<Byte>() {
					@Override
					public int compare(Byte o1, Byte o2) {
						return o1.compareTo(o2);
					}
				});
				for(byte reg : regs) {
					cont.append(cg.pushReg(reg));
				}
			}
		}
			
		prepend(cont);
		if(VERBOSE_LO <= verbose) prepend(new CGIText(";build " + this.getClass().getSimpleName() + " '" + name + "'"));
		
		append(lbEScope);

		if(!isLaunchPoint || !isFirstBlock) {
			//Декрементируем счетчики ссылок
			for(CGVarScope vScope : locals.values()) {
				if(vScope.getType().isClassType() && !vScope.getType().isEnum()) {
					cg.updateClassRefCount(this, vScope.getCells(), false);
				}
				else if(vScope.getType().isArray()) {
					cg.updateArrRefCount(this, vScope.getCells(), false, vScope.isArrayView());
				}
			}
		}
		
		if(!isLaunchPoint && isFirstBlock) {
			if(!regs.isEmpty()) {
				Collections.sort(regs, new Comparator<Byte>() {
					@Override
					public int compare(Byte o1, Byte o2) {
						return o2.compareTo(o1);
					}
				});
				for(byte reg : regs) {
					append(cg.popReg(reg));
				}
			}
		}
		if(!isLaunchPoint || !isFirstBlock) {
			if(!isFirstBlock && 0!=stackOffset) {
				append(cg.blockFree(stackOffset));
			}
		}

		if(0!=mScope.getArgsStackSize() || 0!=stackOffset) {
//			cg.jump(cg.getScope(), new CGLabelScope(null, -1, "j8bproc_vars_free", true));
		}
		
/*		if(null != mScope && (0 != stackOffset || 0 != varSize)) {
			cg.moveIReg(cg.getScope(), 'y', (stackOffset+mScope.getVarSize())*(-1));
		}*/

		
		if(isFirstBlock) {
			if(isLaunchPoint) {
				cg.terminate(this, true, !excs.getProduced().isEmpty());
			}
			else {
				CGClassScope cScope = (CGClassScope)mScope.getParent();
				// Для нити отдельная процедура выхода - просто ret
				if((InstanceType.THREAD==cScope.getInstType()  || InstanceType.TIMER==cScope.getInstType()) && mScope.getSignature().equals("run()")) {
					append(cg.eReturn(null, VarType.VOID, null));
				}
				else {
					append(cg.finMethod(mScope.getType(), mScope.getArgStackSize(), stackOffset, mScope.getLastStackOffset()));
				}
			}
		}
		//if(isFirstBlock && 0!=stackOffset) {
			//append(cg.eReturn(null, mScope.getStackSize(), stackOffset, mScope.getType()));
		//	append(cg.popStackReg(null));
		//}
		
		if(OptimizationType.FRONT.getId()<cg.getDevice().getOptimizationType().getId()) {
			CodeOptimizer co = cg.getOptimizer();
			if(null != co) {
				co.optimizeJumpChains(this);
//				co.optimizeBranchChains(this);
				co.optimizePushConst(this, 'Y');
				co.optimizePushConst(this, 'Z');
				co.optimizeAfterJump(this);
				co.optimizeRegLoad(parent);
				co.optimizeBaseInstr(this);
			}
		}
		
		if(VERBOSE_LO <= verbose) append(new CGIText(";block end"));
	}

	CGMethodScope getMethodScope() {
		CGScope result = this;
		while(!(result instanceof CGMethodScope)) {
			result = result.getParent();
		}
		return (CGMethodScope)result;
	}
	
	public CGLabelScope addLabel(String name) {
		labels.add(name);
		CGLabelScope result = new CGLabelScope(this, genId(), name, true);
		return result;
	}
	
	public boolean containsLabel(String name) {
		return labels.contains(name);
	}
	
	public void addVar(CGVarScope vScope) {
		locals.put(vScope.getResId(), vScope);
	}
	public CGVarScope getVar(int resId) {
		CGVarScope vScope = locals.get(resId);
		if(null!=vScope) return vScope;

		if(null!=parent) {
			CGScope scope = parent;
		
			if(scope instanceof CGMethodScope) return ((CGMethodScope)scope).getArg(resId);
			if(scope instanceof CGBlockScope) return ((CGBlockScope)scope).getVar(resId);
			if(scope instanceof CGCommandScope) return ((CGBlockScope)scope.getScope(CGBlockScope.class)).getVar(resId);
		}
		return null;
	}

	// Выделение ячеек для переменной в блоке(регистр или стек)
	// Изначально ячейки могли содержать регистры и стек одновременно(если регистров не хватило)
	// Но это очень сильно усложняет логику работы кодогенератора, а также сложно добиться оптимального генерируемого кода
	public CGCells memAllocate(CGScope cgScope, int size) throws CompileException {
		// Пытаемся одолжить регистры у родителя (в этом случае не нужно сохранять их в стек на текущем уровне)
		RegPair[] regPairs = null;
		if(parent instanceof CGBlockScope) {
			CGCells result = ((CGBlockScope)parent).memAllocate(cgScope, size);
			if(null!=result) {
				for(int i=0; i<size; i++) {
					byte regId = (byte)result.getId(i);
					parentUsedRegs.add(regId); // Запоминаем, что одолжили у родителя чтобы в конце блока сообщить ему какие освобождать
					regsPool.get(regId).setFree(false);
				}
				if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated " + result + " from parent block (" + parent.getPath('.') + ") " + System.currentTimeMillis()));
				return result;
			}
		}
		else if(parent instanceof CGMethodScope) {
			regPairs = mScope.borrowReg(size);
			if(null!=regPairs) {
				for(RegPair pair : regPairs) {
					parentUsedRegs.add(pair.getReg()); // Запоминаем, что одолжили у родителя чтобы в конце блока сообщить ему какие освобождать
					regsPool.get(pair.getReg()).setFree(false);
				}
				if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated " + StrUtils.toString(regPairs) + " from method (" + parent.getPath('.') + ") " + System.currentTimeMillis()));
				return new CGCells(regPairs);
			}
		}
		else {
			throw new CompileException("Unexpected parent:" + parent);
		}
		
		// Не смогли одолжить у родителя, занимаем на этом уровне
		if(!regsPool.isEmpty()) {
			if(0x02==size) {
				RegPair[] pairs = findRegsPair(regsPool); // Регистры сразу помечаются занятыми (если не null))
				if(null!=pairs) {
					for(RegPair pair: pairs) {
						localUsedRegs.add(pair.getReg());	// Помечаем, что регистр использовался на этом уровне
					}
					if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated " + StrUtils.toString(pairs) + " locally (" + getPath('.') + ") " +  + System.currentTimeMillis()));
					return new CGCells(pairs);
				}
			}
			else if(0x04==size) {
				RegPair[] pairs1 = findRegsPair(regsPool);
				if(null!=pairs1) {
					RegPair[] pairs2 = findRegsPair(regsPool);
					if(null!=pairs2) {
						for(RegPair pair: pairs1) {
							localUsedRegs.add(pair.getReg());	// Помечаем, что регистр использовался на этом уровне
						}
						for(RegPair pair: pairs2) {
							localUsedRegs.add(pair.getReg());	// Помечаем, что регистр использовался на этом уровне
						}
						regPairs = new RegPair[]{pairs1[0x00], pairs1[0x01], pairs2[0x00], pairs2[0x01]};
						if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated " + StrUtils.toString(regPairs) + " locally (" + getPath('.') + ") " +  + System.currentTimeMillis()));
						return new CGCells(regPairs);
					}
					else {
						pairs1[0x00].setFree(true); // Вторуя пару не нашли - освобождаем первую
						pairs1[0x01].setFree(true);
					}
				}
			}

			// Не удалось занять рядом стоящие регистры, занимаем вразброс
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
							localUsedRegs.add(rp.getReg());	// Помечаем, что регистр использовался
						}
						if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated " + StrUtils.toString(result) + " locally (" + getPath('.') + ") " + System.currentTimeMillis()));
						return new CGCells(result);
					}
				}
				else {
					index=0;
				}
			}
		}

		// Не удалось занять регистры, выделяем память в стек фрейме
		CGCells cells = new CGCells(CGCells.Type.STACK_FRAME, size, stackOffset);
		cells.setObject(this);
		stackOffset+=size;
		if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";allocated x" + size + " from stack frame (" + getPath('.') + ")"));
		return cells;
	}

	public int getVarsStackSize() {
		return stackOffset;
	}
	
	public void releaseRegsPool() throws CompileException {
		for(RegPair pair : regsPool.values()) {
			if(!pair.isFree()) {
				releaseReg(this, pair.getReg());
			}
		}
	}		
	
	protected void releaseReg(CGScope cgScope, Byte regId) throws CompileException {
		if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";release REG[" + regId + "] (" + getPath('.') + ") " + System.currentTimeMillis()));
		regsPool.get(regId).setFree(true);
		if(parentUsedRegs.contains(regId)) {
			if(parent instanceof CGMethodScope) {
				((CGMethodScope)parent).releaseReg(cgScope, regId);
			}
			else if(parent instanceof CGBlockScope) {
				((CGBlockScope)parent).releaseReg(cgScope, regId);
			}
			parentUsedRegs.remove(regId);
		}
	}
	
	public boolean isFirstBlock() {
		return isFirstBlock;
	}
	
	public CGLabelScope getELabel() {
		return lbEScope;
	}

	@Override
	public String getLName() {
		return "";
	}
	
	@Override
	public String toString() {
		return "block " + name;
	}
}
