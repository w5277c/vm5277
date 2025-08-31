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

import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.CodeGenerator;

public class CGConditionScope extends CGCommandScope {
	private CGLabelScope	lbBeginScope;
	private CGLabelScope	lbThenScope;
	private CGLabelScope	lbElseScope;
	private CGLabelScope	lbEndScope;
	private	boolean			mustElseJump;	// Состояние последнего Expression
	
	public CGConditionScope(CodeGenerator cg, CGScope parent) {
		super(parent);
		
		lbBeginScope = cg.makeLabel(null, "_j8b_ifbegin", false);
		lbThenScope = cg.makeLabel(null, "_j8b_ifthen", false);
		lbElseScope = cg.makeLabel(null, "_j8b_ifelse", false);
		lbEndScope = cg.makeLabel(null, "_j8b_ifend", false);
	}
	
	public void mustElseJump(boolean mustElseJump) {
		this.mustElseJump = mustElseJump;
	}
	public boolean mustElseJump() {
		return mustElseJump;
	}
	
	public CGLabelScope getLbBeginScope() {
		return lbBeginScope;
	}
	public CGLabelScope getLbThenScope() {
		return lbThenScope;
	}
	public CGLabelScope getLbElseScope() {
		return lbElseScope;
	}
	public CGLabelScope getLbEndScope() {
		return lbEndScope;
	}
}
