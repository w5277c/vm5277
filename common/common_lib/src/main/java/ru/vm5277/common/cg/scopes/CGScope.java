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
import ru.vm5277.common.cg.items.CGIText;

public class CGScope extends CGIContainer {
	private				static	int						idCntr		= 0;
	protected	final			String					name;
	protected					CGScope					parent;
	protected	final	static	Map<Integer, CGScope>	scopesMap	= new HashMap<>();

	protected					int						resId;
	private						int						sbPos		= 0;
	private						boolean					verbose		= false;
	
	
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
			if(!(this instanceof CGLabelScope)) {
				parent.append(this);
			}
			scopesMap.put(resId, this);
		}
		if(verbose) append(new CGIText(";======== enter " + toString() + " ========"));
	}
	
	public CGScope getParent() {
		return parent;
	}
	
	public int getResId() {
		return resId;
	}
	
	public CGScope free() {
		if(verbose) append(new CGIText(";======== leave " + toString() + " ========"));
//!!!		parent.asmAppend(cgb);
		return parent;
	}
	
	public CGScope getScope(int resId) {
		return scopesMap.get(resId);
	}
	
	public String getPath() {
		StringBuilder sb = new StringBuilder();
		CGScope _scope = this;
		while(null != _scope) {
			if(!_scope.getName().isEmpty()) {
				sb.insert(0, _scope.getName() + "_");
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
}
