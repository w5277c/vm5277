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
import ru.vm5277.common.cg.CGBranch;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.cg.RegPair;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.exceptions.CompileException;

public class CGScope extends CGIContainer {
	public		final	static	int						VERBOSE_NO	= 0;
	public		final	static	int						VERBOSE_LO	= 1;
	public		final	static	int						VERBOSE_HI	= 2;
	private				static	int						idCntr		= 0;
	protected					String					name;
	protected					CGScope					parent;
	protected					CGScope					bottom		= null;
	protected					CGBranch				branch		= null;
	
	protected					Set<CGVarScope>			liveVars	= new HashSet<>(); // Живые пермененны этого scope
	
	protected					int						resId;
	private						int						sbPos		= 0;
	public				static	int						verbose;

	public static synchronized int genId() {
		return idCntr++;
	}

	public static void launchPointActivate() {
		idCntr=10000;
	}

	public CGScope() {
		name = "";
	}
	
	public CGScope(CGScope parent, int resId, String name) {
		this.parent = parent;
		this.name = name;
		this.resId = resId;
		
		if(null != parent) {
			parent.append(this);
		}
	}
	
	public CGScope(CGScope parentScope, int resId, CGScope oldScope, String name) {
		this.parent = parentScope;
		this.name = name;
		this.resId = resId;
		
		if(null!=parentScope) {
			if(null!=oldScope) {
				parentScope.replace(oldScope, this);
			}
			else {
				parentScope.append(this);
			}
		}
	}

	public CGScope getParent() {
		return parent;
	}
	public void swap(CGScope cgScope1, CGScope cgScope2) throws CompileException {
		int index1 = items.indexOf(cgScope1);
		if(-1==index1) {
			throw new CompileException("COMPILER BUG: CGScope " + cgScope1 + " not found in parent");
		}
		int index2 = items.indexOf(cgScope2);
		if(-1==index2) {
			throw new CompileException("COMPILER BUG: CGScope " + cgScope2 + " not found in parent");
		}
		items.set(index1, cgScope2);
		items.set(index2, cgScope1);
	}
	
	public int getResId() {
		return resId;
	}
	
	public CGScope getBottom() {
		if(null==bottom) bottom = new CGScope();
		return bottom;
	}
	public boolean bottomUsed() {
		return null!=bottom && !bottom.items.isEmpty();
	}
	
	public String getPath(Character delimeter) {
		StringBuilder sb = new StringBuilder();
		CGScope _scope = this;
		while(null != _scope) {
			if(!_scope.getName().isEmpty()) {
				sb.insert(0, _scope.getName() + (null != delimeter ? delimeter : ""));
				//TODO костыль, но пока работает, иначе все пути начинаются с Main.
				if(_scope instanceof CGClassScope && ((CGClassScope)_scope).isImported()) {
					break;
				}
			}
			_scope = _scope.getParent();
		}
		if(null != delimeter) sb.deleteCharAt(sb.length()-0x01);
		return sb.toString();
	}

	public String getLPath() {
		StringBuilder sb = new StringBuilder();
		CGScope _scope = this;
		while(null != _scope) {
			if(!_scope.getLName().isEmpty()) {
				sb.insert(0, _scope.getLName());
				if(_scope instanceof CGClassScope && ((CGClassScope)_scope).isImported()) {
					break;
				}
			}
			_scope = _scope.getParent();
		}
		return sb.toString();
	}
	
	public String getName() {
		return name;
	}
	public String getLName() {
		return name;
	}
	
	public<T> CGScope getScope(Class<T> clazz) {
		CGScope _scope = this;
		while(null!=_scope) {
			if(clazz.isInstance(_scope)) return _scope;
			_scope = _scope.getParent();
		}
		return null;
	}
	
	// Вероятно эта парная аллокация полезна только для AVR. Похоже нужно будет вынести в библиотеку кодогенератора
	protected RegPair[] findRegsPair(Set<RegPair> usedRegsShot, HashMap<Byte, RegPair> regsPool) throws CompileException {
		List<RegPair> tmp = new ArrayList<>(regsPool.values());
		Collections.sort(tmp);

		for(RegPair pair1 : tmp) {
			if(pair1.isFree() && 0==(pair1.getReg()&0x01) && (null==usedRegsShot || !usedRegsShot.contains(pair1))) {
				for(RegPair pair2 : tmp) {
					if(pair2.isFree() && pair1.getReg()+0x01==pair2.getReg() && (null==usedRegsShot || !usedRegsShot.contains(pair2))) {
						pair1.setFree(false);
						pair2.setFree(false);
						return new RegPair[]{pair1, pair2};
					}
				}
			}
		}
		return null;
	}
	
	public int getVerbose() {
		return verbose;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + (name.isEmpty() ? "" :  ":" + name);
	}
	
	public void setBranch(CGBranch branch) {
		this.branch = branch;
	}
	public CGBranch getBranch() {
		return branch;
	}
	
	public Set<CGVarScope> getLiveVars() {
		return liveVars;
	}
	
	public void buildScopeTree(StringBuilder sb, String prefix, boolean parentDisabled) {
		if(disabled || parentDisabled) {
			sb.append("#");
		}
		sb.append(prefix).append(toString()).append("\n");
		for(CGItem item : items) {
			if(item instanceof CGScope) {
				((CGScope)item).buildScopeTree(sb, prefix + "  ", disabled | parentDisabled);
			}
		}
	}
	
	@Override
	public String getSource() throws CompileException {
		
		StringBuilder sb = new StringBuilder();
		sb.append(super.getSource());
		if(null!=bottom && !bottom.items.isEmpty()) {
			sb.append(bottom.getSource());
		}
		return sb.toString();
	}
}
