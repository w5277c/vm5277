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
package ru.vm5277.common.cg;

public class CGScope {
	private		final			String			name;
	private						CGScope			parent;
	protected					int				resId;
	private						int				sbPos	= 0;
	protected					StringBuilder	sb		= new StringBuilder();
	
	public CGScope(CGScope parent, int resId, String name) {
		this.parent = parent;
		this.name = name;
		this.resId = resId;
	}
	
	public CGScope getParent() {
		return parent;
	}
	
	public int getResId() {
		return resId;
	}
	
	public void asmAppend(String str) {
		sb.append(str);
		sbPos = sb.length()-1;
	}
	public void asmInsert(String str) {
		sb.insert(sbPos, str);
		sbPos += str.length();
	}
	public void asmPrepend(String str) {
		sb.insert(0, str);
		sbPos = str.length();
	}
	public String getAsm() {
		return sb.toString();
	}
	
	public CGScope free() {
		parent.asmAppend(sb.toString());
		return parent;
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
	
	public CGMethodScope getMethodScope() {
		CGScope _scope = this;
		while(null != _scope) {
			if(_scope instanceof CGMethodScope) return (CGMethodScope)_scope;
			_scope = _scope.getParent();
		}
		return null;
	}
}
