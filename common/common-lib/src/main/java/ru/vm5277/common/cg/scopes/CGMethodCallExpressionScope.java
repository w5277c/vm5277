/*
 * Copyright 2026 konstantin@5277.ru
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
import ru.vm5277.common.cg.items.CGIText;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.SourcePosition;

public class CGMethodCallExpressionScope extends CGExpressionScope {
	public CGMethodCallExpressionScope(CGScope parent, SourcePosition sp, CGScope oldScope, String name) {
		super(parent, sp, oldScope, name);
	}

	public void releaseRef(CodeGenerator cg, CGScope cgScope, VarType type, CGCells cells) throws CompileException {
		if(type.isClassType() && !type.isEnum()) {
			if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";release class '" + name + "', cells:" + cells.toString()));
			cg.updateClassRefCount(cgScope, cells, false);
		}
		else if(type.isArray()) {
			if(VERBOSE_LO <= verbose) cgScope.append(new CGIText(";release array '" + name + "', cells:" + cells.toString()));
			cg.updateArrRefCount(cgScope, cells, false);
		}
	}
}
