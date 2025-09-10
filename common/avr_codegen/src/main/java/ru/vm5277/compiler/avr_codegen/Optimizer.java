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
package ru.vm5277.compiler.avr_codegen;

import java.util.ArrayList;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.CodeOptimizer;
import ru.vm5277.common.cg.items.CGIAsmJump;
import ru.vm5277.common.cg.items.CGIContainer;
import ru.vm5277.common.cg.items.CGItem;
import ru.vm5277.common.cg.scopes.CGExpressionScope;
import ru.vm5277.common.cg.scopes.CGLabelScope;
import ru.vm5277.common.cg.scopes.CGScope;

public class Optimizer extends CodeOptimizer {
	
	
	@Override
	public void optimizeBranchChains(CGScope scope) {
		ArrayList<CGItem> list = new ArrayList();

		boolean changed = true;
		while(changed) {
			list.clear();
			
			optimizeEmptyJumps(scope);
			CodeGenerator.treeToList(scope, list);		

			changed = false;
			for(int i=0; i<list.size()-2; i++) {
				CGItem item1 = list.get(i);
				CGItem item2 = list.get(i+1);
				CGItem item3 = list.get(i+2);
				if(item1 instanceof CGIAsmJump && item2 instanceof CGIAsmJump && item3 instanceof CGLabelScope) {
					CGIAsmJump j1 = (CGIAsmJump)item1;
					CGIAsmJump j2 = (CGIAsmJump)item2;
					String name = ((CGLabelScope)item3).getName();
					
					if(j1.getText().toLowerCase().startsWith("br") && j2.getText().equalsIgnoreCase("rjmp") && j1.getLabelName().equalsIgnoreCase(name)) {
						j1.setText(Utils.brInstrInvert(j1.getText()));
						j1.setLabelName(j2.getLabelName());
						j2.disable();
						changed = true;
					}
				}
			}
		}		
	}


	//TODO рудимент?
	//Блок оптимизации, вырезает часть команд из условия сравнения преременной с константой, возвращает инструкцию для перехода на false блок
	public static String localWithConstantComparingCondition(CGExpressionScope eScope) {
		String result = null;
		CGIContainer cont = (CGIContainer)eScope.getItems().get(eScope.getItems().size()-1);
		if(cont.getTag().equals("constAction:LT") || cont.getTag().equals("constAction:LTE")) {
			cont.getItems().remove(cont.getItems().size()-1);
			result = "brcs";
		}
		else if(cont.getTag().equals("constAction:GT") || cont.getTag().equals("constAction:GTE")) {
			cont.getItems().remove(cont.getItems().size()-1);
			cont.getItems().remove(cont.getItems().size()-1);
			result = "brcc"; //TODO проверить
		}
		return result;
	}
	
	
}
