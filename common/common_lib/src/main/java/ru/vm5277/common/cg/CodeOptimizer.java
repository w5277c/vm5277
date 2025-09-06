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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.vm5277.common.LabelNames;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIAsmLdLabel;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;

public abstract class CodeOptimizer {

	public abstract void optimizeBranchChains(CGScope scope);
	
	protected boolean optimizeEmptyJumps(CGScope scope) {
		boolean result = false;
		
		ArrayList<CGItem> list = new ArrayList();
		boolean changed =true;
		while(changed) {
			list.clear();
			treeToList(scope, list);
		
			changed = false;
			for(int i=0; i<list.size()-1; i++) {
				CGItem item1 = list.get(i);
				if(item1 instanceof CGIAsmJump) {
					CGIAsmJump jump = (CGIAsmJump)item1;
					CGItem item2 = list.get(i+1);
					if(item2 instanceof CGLabelScope) {
						String name = ((CGLabelScope)item2).getName();
						if(jump.getLabelName().equalsIgnoreCase(name)) {
							jump.disable();
							changed = true;
							result = true;
						}
					}
				}
			}
		}
		return result;
	}
	
	protected void removeUnusedLabels(CGScope scope) {
		ArrayList<CGItem> list = new ArrayList();
		Map<String, CGLabelScope> labels = new HashMap<>();
		Set<String> usedLabels = new HashSet<>();
		
		treeToList(scope, list);
		
		for(int i=0; i<list.size(); i++) {
			CGItem item = list.get(i);
			if(item instanceof CGIAsmJump) {
				usedLabels.add(((CGIAsmJump)item).getLabelName().toLowerCase());
			}
			else if(item instanceof CGIAsmLdLabel) {
				usedLabels.add(((CGIAsmLdLabel)item).getLabelName().toLowerCase());
			}
			else if(item instanceof CGLabelScope) {
				CGLabelScope label = (CGLabelScope)item;
				if(label.isUsed()) {
					labels.put(label.getName().toLowerCase(), label);
				}
			}
		}
		
		for(String labelName : labels.keySet()) {
			if(!usedLabels.contains(labelName)) {
				CGLabelScope lbScope = labels.get(labelName);
				lbScope.disable();
			}
		}
	}

	
	public void optimizeJumpChains(CGScope scope) {
		ArrayList<CGItem> list = new ArrayList();
		Map<String,String> replacementMap = new HashMap<>();

		while(true) {
			list.clear();
			replacementMap.clear();
			
			optimizeEmptyJumps(scope);
			treeToList(scope, list);

			for(int i=1; i<list.size()-1; i++) {
				CGItem item = list.get(i);
				if(item instanceof CGLabelScope) {
					String name = ((CGLabelScope)item).getName();
					if(list.get(i-1) instanceof CGIAsmJump) {
						CGItem nextItem = list.get(i+1);
						if(nextItem instanceof CGIAsmJump) {
							replacementMap.put(name, ((CGIAsmJump)nextItem).getLabelName());
							item.disable();
							nextItem.disable();
						}
					}
				}
			}

			if(replacementMap.isEmpty()) {
				removeUnusedLabels(scope);
				break;
			}
			
			boolean changed=true;
			while(changed) {
				changed = false;
				for (String source : replacementMap.keySet()) {
					String target = replacementMap.get(source);
					String newTarget = replacementMap.get(target);
					if(null != newTarget) {
						replacementMap.put(source, newTarget);
						changed = true;

					}
				}
			}

			for(CGItem item : list) {
				if(!item.isDisabled() && item instanceof CGIAsmJump) {
					CGIAsmJump oldJump = (CGIAsmJump)item;
					String target = replacementMap.get(oldJump.getLabelName());
					if(null != target) {
						oldJump.setLabelName(target);
					}
				}
			}
		}		
	}

	public static void treeToList(CGIContainer cont, List<CGItem> list) {
		for(CGItem item : cont.getItems()) {
			if(item.isDisabled()) continue;
			
			if(item instanceof CGIContainer) {
				if(((CGIContainer)item).getItems().isEmpty()) {
					list.add(item);
				}
				else {
					treeToList((CGIContainer)item, list);
				}
			}
			else {
				list.add(item);
			}
		}
	}
}
