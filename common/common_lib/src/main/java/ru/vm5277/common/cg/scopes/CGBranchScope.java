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

import java.util.Stack;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;

public class CGBranchScope extends CGScope {
	private Stack<CGLabelScope>	lbEndScopes	= new Stack<>();	// Выход из текущего подвыражения
	private	boolean				isUsed		= false;
	
	public CGBranchScope(CodeGenerator cg, CGScope parent) {
		super(parent, -1, null);
		
		lbEndScopes.add(new CGLabelScope(null, null, LabelNames.COMPARE_END, true));
	}
	
	public void pushEnd(CGLabelScope lbScope) {
		lbEndScopes.add(lbScope);
	}
	public CGLabelScope popEnd() {
		return lbEndScopes.pop();
	}
	
	public CGLabelScope getEnd() {
		isUsed = true;
		return lbEndScopes.lastElement();
	}
	
	public boolean isUsed() {
		return isUsed;
	}
}
