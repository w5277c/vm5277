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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;

public class CGTryBlockScope extends CGBlockScope {
	private			Set<Integer>					exceptionIds	= new HashSet<>();
	private			List<CGLabelScope>				catchLabels		= new ArrayList<>();
	private			Map<CGLabelScope, Set<Integer>>	catches			= new HashMap<>();
	
	public CGTryBlockScope(CodeGenerator cg, CGScope parent, int id) {
		super(cg, parent, id);
	}

	public void addCatchExceptions(Set<Integer> ids) {
		exceptionIds.addAll(ids);
		CGLabelScope lbScope = new CGLabelScope(null, null, LabelNames.CATCH, true);
		catchLabels.add(lbScope);
		catches.put(lbScope, ids);
	}
	public boolean containsExpception(int id) {
		return exceptionIds.contains(id);
	}
	public boolean containsException(CGLabelScope lbScope, int id) {
		Set<Integer> ids = catches.get(lbScope);
		if(null!=ids) {
			return ids.contains(id);
		}
		return false;
	}
	
	public Set<Integer> getExceptionIds() {
		return exceptionIds;
	}
	
	public Set<Integer> getExceptionIds(CGLabelScope lbScope) {
		return catches.get(lbScope);
	}


	public List<CGLabelScope> getCatchLabels() {
		return catchLabels;
	}
	
	public CGLabelScope getCatchLabel(int id) throws CompileException  {
		for(CGLabelScope lbScope : catches.keySet()) {
			Set<Integer> ids = catches.get(lbScope);
			if(ids.contains(id)) return lbScope;
		}
		throw new CompileException("COMPILER ERROR: Can't find label scope for exception id:" + id);
	}
}
