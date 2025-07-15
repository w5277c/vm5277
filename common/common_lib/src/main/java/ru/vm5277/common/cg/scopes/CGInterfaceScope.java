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

import ru.vm5277.common.compiler.VarType;

public class CGInterfaceScope extends CGScope {
	private	final	VarType	type;
	private	final	int[]	intrerfaceIds;
			
	public CGInterfaceScope(CGScope parent, int id, VarType type, int[] intrerfaceIds, String name) {
		super(parent, id, name);
		
		this.type = type;
		this.intrerfaceIds = intrerfaceIds;
	}
}
