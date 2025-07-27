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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGIText;
import static ru.vm5277.common.cg.scopes.CGScope.verbose;
import ru.vm5277.common.exceptions.CompileException;

//TODO необходимо перевыделять блок в стеке, также нужно передавать размер выделенного блока как смещение для внутренних переменных
//TODO а в будущем, учитывать свободные регистры

public class CGBlockScope extends CGScope {
	private		final	Map<Integer, CGVarScope>	locals				= new HashMap<>();
	protected			int							stackBlockOffset	= 0;
//	protected	final	List<Byte>					usedRegs;	// Регистры использованные из списка свободных
	private				Set<RegPair>				usedPool			= new HashSet<>(); // Использованные регистры из regsPool метода
	private				CGMethodScope				mScope;
	
	public CGBlockScope(CGScope parent, int id) {
		super(parent, id, "");
		
//		this.usedRegs = new ArrayList<>();
		
		CGBlockScope bScope = parent.getBlockScope();
		if(null != bScope) {
			stackBlockOffset = bScope.getStackBlockSize();
		}
		
		mScope = parent.getMethodScope();
	}
	
	public void build(CodeGenerator cg) throws CompileException {
		CGIContainer cont = new CGIContainer();
		if(VERBOSE_LO <= verbose) cont.append(new CGIText(";build block " + getPath('.') + ",id:" + resId));
		
// Кажется это не нужно, в блоке мы работаем с регистрами взятыми из regsPool, их не нужно сохранять, кроме регистров аккумулятора(но это в другой раз)
// Регистры(если используются в качестве переменных) используемые в нативных вызовах должны быть сохранены в методе invokeNative
/*		for(int i=0; i<usedRegs.size(); i++) {
			cont.append(cg.pushRegAsm(usedRegs.get(i)));
		}
*/		
		if(0x00 != stackBlockOffset) {
			cg.setDpStackAlloc();
			cont.append(cg.stackAllocAsm(stackBlockOffset));
		}
		prepend(cont);
		
/*
		for(int i=usedRegs.size()-1; i>=0; i--) {
			append(cg.popRegAsm(usedRegs.get(i)));
		}*/
		if(0x00 != stackBlockOffset) append(cg.stackFreeAsm());
	}

	
	public void addLocal(CGVarScope local) {
		locals.put(local.getResId(), local);
	}
	public CGVarScope getLocal(int resId) {
		CGVarScope lScope = locals.get(resId);
		if(null != lScope) return lScope;
		
		if(null != parent && parent instanceof CGBlockScope) return ((CGBlockScope)parent).getLocal(resId);
		return null;
	}

	public int getStackBlockSize() {
		return stackBlockOffset;
	}

	/*public void putUsedReg(byte reg) {
		if(16==reg) return; //r16 всегда свободен и используется для аккумулятора(который используется только временно(для выражений))
		if(!usedRegs.contains(reg)) {
			if(parent instanceof CGBlockScope) { // Если родитель тоже блок, и он не использует этот регистр, то выгодней сохранение регистра вынести в родитель
				CGBlockScope bScope =((CGBlockScope)parent);
				if(bScope.getUsedRegs().contains(reg)) {
					usedRegs.add(reg);
				}
				else {
					bScope.putUsedReg(reg);
				}
			}
			else usedRegs.add(reg);
		}
	}
	public void putUsedRegs(byte[] regs) {
		for(byte reg : regs) {
			putUsedReg(reg);
		}
	}
	public List<Byte> getUsedRegs() { // только для чтения!
		return usedRegs;
	}*/

	// Выделение ячеек для переменной в блоке(регистр или стек)
	public CGCell[] memAllocate(int size) throws CompileException {
		CGCell[] cells = new CGCell[size];
		for(int i=0; i<size; i++) {
			RegPair regPair = null == mScope ? null : mScope.borrowReg();
			if(null != regPair) {
				// putUsedReg(reg); не нужно
				usedPool.add(regPair);
				cells[i] = new CGCell(CGCell.Type.REG, regPair.getReg());
			}
			else {
				cells[i] = new CGCell(CGCell.Type.STACK, stackBlockOffset++);
			}
		}
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
	
	@Override
	public String getLName() {
		return "B" + resId;
	}
}
