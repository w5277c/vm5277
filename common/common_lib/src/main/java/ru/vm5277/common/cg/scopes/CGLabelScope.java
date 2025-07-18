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
	private	static	int		idCntr	= 0;
	private			boolean	isUsed;
	
	
	public CGLabelScope(CGScope scope, Integer resId, String name, boolean isUsed) {
		super();
		
		this.resId = (null == resId ? idCntr++ : resId);
		this.name = "JAVL" + (null == scope ? "" : scope.getLPath()) + resId + (null != name ? name : "");
		this.isUsed = isUsed;
	}

	public void setUsed() {
		isUsed = true;
	}
	
	@Override
	public String getSource() {
		return isUsed ? name + ":\n" : "";
	}
}
