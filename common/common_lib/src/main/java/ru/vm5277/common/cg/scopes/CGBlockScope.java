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
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import static ru.vm5277.common.cg.scopes.CGScope.verbose;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.common.exceptions.CompileException;

// STACK(reg Y) - необходимо выделять блок памяти в стеке для локальных переменных, Y не изменяем. Y изменяется только если выходим за 64 слова, затем
// его нужно восстановить. Смещения адресов локальных перменных высчитывааются от начала выделенного блока в методе


public class CGBlockScope extends CGScope {
	private				CGLabelScope				lbEScope;
	private		final	Map<Integer, CGVarScope>	locals		= new HashMap<>();
	protected			int							stackOffset	= 0; // Хранит размер блока в стеке, выделенный под локальные переменные
	private				Set<RegPair>				usedPool	= new HashSet<>(); // Использованные регистры из regsPool метода
	private				CGMethodScope				mScope;
	private				List<Byte>					usedRegs	= new ArrayList<>();
	private				boolean						isFirstBlock;
	private				Set<String>					labels		= new HashSet<>();
	
	public CGBlockScope(CodeGenerator cg, CGScope parent, int id) {
		super(parent, id, "");
		
/*		CGBlockScope bScope = parent.getBlockScope();
		if(null != bScope) {
			this.stackOffset = bScope.getStackBlockSize();
		}
*/		
		if(parent instanceof CGMethodScope) isFirstBlock = true;

		mScope = (CGMethodScope)parent.getScope(CGMethodScope.class);
		mScope.addBlockScope(this);

		lbEScope = new CGLabelScope(null, null, LabelNames.BLOCK_END, true);
//		cg.addLabel(lbEScope);
	}
	
	public void build(CodeGenerator cg, boolean isLaunchPoint) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";build block"));

		if(0!=mScope.getArgsStackSize() || 0!=stackOffset) {
			cont.append(cg.stackAlloc(isFirstBlock, mScope.getArgsStackSize(), stackOffset));
		}

		// Регистры(если используются в качестве переменных) используемые в нативных вызовах должны быть сохранены в методе invokeNative
		if(!isLaunchPoint) {
			for(int i=0; i<usedRegs.size(); i++) {
//TODO Не верно, факт использования регистра должна быть передана в метод, вызывающему этот метод нужно решить, сохранять регистр или нет.
				//cont.append(cg.pushRegAsm(usedRegs.get(i)));
			}
		}
			
		prepend(cont);

		append(lbEScope);
		if(!isLaunchPoint) {
			for(int i=usedRegs.size()-1; i>=0; i--) {
				//TODO Не верно, факт использования регистра должна быть передана в метод, вызывающему этот метод нужно решить, сохранять регистр или нет.
//				append(cg.popRegAsm(usedRegs.get(i)));
			}
		}
		
		//Декрементируем счетчики ссылок
		for(CGVarScope vScope : locals.values()) {
			if(vScope.getType().isClassType() && !vScope.getType().isEnum()) {
				cg.updateClassRefCount(this, vScope.getCells(), false);
			}
			else if(vScope.getType().isArray()) {
				cg.updateArrRefCount(this, vScope.getCells(), false, vScope.isArrayView());
			}
		}
		if(!isFirstBlock && 0!=stackOffset) {
			append(cg.blockFree(stackOffset));
		}

		if(0!=mScope.getArgsStackSize() || 0!=stackOffset) {
//			cg.jump(cg.getScope(), new CGLabelScope(null, -1, "j8bproc_vars_free", true));
		}
		
/*		if(null != mScope && (0 != stackOffset || 0 != varSize)) {
			cg.moveIReg(cg.getScope(), 'y', (stackOffset+mScope.getVarSize())*(-1));
		}*/

		
		if(isFirstBlock) {
			append(cg.finMethod(mScope.getType(), mScope.getArgStackSize(), stackOffset, mScope.getLastStackOffset()));
		}
		//if(isFirstBlock && 0!=stackOffset) {
			//append(cg.eReturn(null, mScope.getStackSize(), stackOffset, mScope.getType()));
		//	append(cg.popStackReg(null));
		//}
		
		if(Optimization.FRONT<cg.getOptLevel()) {
			CodeOptimizer co = cg.getOptimizer();
			if(null != co) {
				co.optimizeJumpChains(this);
				co.removeUnusedLabels(this);
				co.optimizeBranchChains(this);
				co.optimizePushConst(this, 'Y');
				co.optimizePushConst(this, 'Z');
				co.optimizeBaseInstr(this);
			}
		}
		
		if(VERBOSE_LO <= verbose) append(new CGIText(";block end"));
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

/*	public int getStackSize() {
		if(parent instanceof CGBlockScope) {
			return ((CGBlockScope)parent).getStackSize() + stackOffset;
		}
		return ((CGMethodScope)parent).getArgsStackSize() + 0x02 + stackOffset; //TODO cg.getCallStackSize
	}
*/
	public void putUsedReg(byte reg) {
		if(16==reg) return; //r16 всегда свободен и используется для аккумулятора(который используется только временно(для выражений))
		if(!usedRegs.contains(reg)) {

			//Этот код не сегда работает, так как регистр может быть занят ранее по сурсу но позже по выполнению кодогенерации. Эта задача оптимизатора
/*			if(parent instanceof CGBlockScope) { // Если родитель тоже блок, и он не использует этот регистр, то выгодней сохранение регистра вынести в родитель
				CGBlockScope bScope =((CGBlockScope)parent);
				if(bScope.getUsedRegs().contains(reg)) {
					usedRegs.add(reg);
				}
				else {
					bScope.putUsedReg(reg);
				}
			}
			else usedRegs.add(reg);
*/
			usedRegs.add(reg);
		}
	}
	public void putUsedRegs(byte[] regs) {
		for(byte reg : regs) {
			putUsedReg(reg);
		}
	}
	public List<Byte> getUsedRegs() { // только для чтения!
		return usedRegs;
	}

	// Выделение ячеек для переменной в блоке(регистр или стек)
	// Изначально ячейки могли содержать регистры и стек одновременно(если регистров не хватило)
	// Но это очень сильно усложняет логику работы кодогенератора, а также сложно добиться оптимального генерируемого кода
	public CGCells memAllocate(int size) throws CompileException {
		RegPair[] regPairs = null == mScope ? null : mScope.borrowReg(size);
		if(null != regPairs) {
			for(int i=0; i<size; i++) {
				usedPool.add(regPairs[i]);
				putUsedReg(regPairs[i].getReg());
			}
			
			return new CGCells(regPairs);
		}
		//CGCells cells = new CGCells(CGCells.Type.STACK_FRAME, size, getStackSize());
		CGCells cells = new CGCells(CGCells.Type.STACK_FRAME, size, stackOffset);
		cells.setObject(this);
		stackOffset+=size;
		return cells;
	}

/*
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
*/	
	public int getVarsStackSize() {
		return stackOffset;
	}
	
	public void restoreRegsPool() {
		if(!usedPool.isEmpty()) {
			if(VERBOSE_LO <= verbose) append(new CGIText(";restore regs:" + StrUtils.toString(usedPool)));
			for(RegPair regPair : usedPool) {
				regPair.setFree(true);
			}
		}
	}
	
	public boolean isFreeReg(byte reg) {
		RegPair regPair = mScope.getReg(reg);
		return null == regPair || regPair.isFree();
	}
	
	public CGLabelScope getELabel() {
		lbEScope.setUsed();
		return lbEScope;
	}

	@Override
	public String getLName() {
		return "B" + resId;
	}
}
