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

public class CGLabelScope extends CGScope {
	private			boolean	isUsed;
	private			boolean	isPersist	= false;
	
	public CGLabelScope(CGScope scope, Integer resId, String name, boolean isUsed) {
		super();
		
		this.resId = (null == resId ? genId() : resId);
		this.name = (null == scope ?	((null!=name ? name : "") + (-1==this.resId ? "" : "_" + this.resId)) :
										("j8b_" + scope.getLPath()) + "_" + (null!=name ? name  + "_" : "") + this.resId);
		this.isUsed = isUsed;
	}

	public CGLabelScope(CGLabelScope parent, String postfix, int offset) {
		super();
		
		this.resId = parent.getResId();
		this.name = parent.getName() + (null==postfix || postfix.isEmpty() ? "" : "_" + postfix) + (0==offset ? "" : "+" + offset);
		this.isUsed = true;
	}

	public void setUsed() {
		isUsed = true;
	}
	public boolean isUsed() {
		return isUsed;
	}
	
	public void setPersist() {
		this.isPersist = true;
	}
	public boolean isPersist() {
		return isPersist;
	}
	
	
	@Override
	public String getSource() {
		return isUsed ? name + ":\n" : "";
	}
	
	@Override
	public String toString() {
		return "label " + name;
	}
}
