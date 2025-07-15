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
import java.util.Map;
import ru.vm5277.common.cg.items.CGIContainer;

public class CGScope extends CGIContainer {
	private				static	int						idCntr		= 0;
	protected					String					name;
	protected					CGScope					parent;
	protected	final	static	Map<Integer, CGScope>	scopesMap	= new HashMap<>();
	protected			static	int						statOffset	= 0; // Блок для хранения статических полей классов
	
	protected					int						resId;
	private						int						sbPos		= 0;
	protected					boolean					verbose		= true;
	
	public static int genId() {
		return idCntr++;
	}

	public CGScope() {
		name = "root";
	}
	
	public CGScope(CGScope parent, int resId, String name) {
		this.parent = parent;
		this.name = name;
		this.resId = resId;
		
		
		if(null != parent) {
			parent.append(this);
			scopesMap.put(resId, this);
		}
	}
	
	public CGScope getParent() {
		return parent;
	}
	
	public int getResId() {
		return resId;
	}
	
	public CGScope free() {
//!!!		parent.asmAppend(cgb);
		return parent;
	}
	
	public CGScope getScope(int resId) {
		return scopesMap.get(resId);
	}
	
	public String getPath(String delimeter) {
		StringBuilder sb = new StringBuilder();
		CGScope _scope = this;
		while(null != _scope) {
			if(!_scope.getName().isEmpty()) {
				sb.insert(0, _scope.getName() + delimeter);
			}
			_scope = _scope.getParent();
		}
		sb.deleteCharAt(sb.length()-0x01);
		return sb.toString().toLowerCase();
	}
	
	public String getName() {
		return name;
	}
	
	public CGBlockScope getBlockScope() {
		CGScope _scope = this;
		while(null != _scope) {
			if(_scope instanceof CGBlockScope) return (CGBlockScope)_scope;
			_scope = _scope.getParent();
		}
		return null;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + name;
	}
	
	@Override
	public String getSource() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(super.getSource());

		return sb.toString();
	}
}
