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

import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;

public class CGLoopBlockScope extends CGBlockScope {
	private	CGLabelScope	startLbScope;
	private	CGLabelScope	nextLbScope;
	private	CGLabelScope	endLbScope;
	
	public CGLoopBlockScope(CodeGenerator cg, CGScope parent, int id, String comment) {
		super(cg, parent, id, comment);
		
		startLbScope = new CGLabelScope(null, null, LabelNames.LOOP, true);
		nextLbScope = new CGLabelScope(null, null, LabelNames.LOOP_NEXT, false);
		endLbScope = new CGLabelScope(null, null, LabelNames.LOOP_END, false);
	}
	
	public CGLabelScope getStartLbScope() {
		return startLbScope;
	}

	public CGLabelScope getNextLbScope() {
		return nextLbScope;
	}

	public CGLabelScope getEndLbScope() {
		return endLbScope;
	}
}
