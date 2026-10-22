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

import ru.vm5277.common.VarType;
import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.lexer.SourcePosition;

public class CGInterfaceScope extends CGScope {
	private	final	CodeGenerator		cg;
	private	final	VarType				type;
			
	public CGInterfaceScope(CodeGenerator cg, CGScope parent, SourcePosition sp, int id, VarType type, String name)  {
		super(parent, sp, id, name);
		
		this.cg = cg;
		this.type = type;
	}

	public CGCells memAllocate(int size) {
		return new CGCells(CGCells.Type.STAT, size, cg.getAndAddStatPoolSize(size));
	}
	
	@Override
	public String getLName() {
		return "I" + name;
	}
}
