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
package ru.vm5277.compiler;

import ru.vm5277.common.cg.CGExcs;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.ObjectTypeNode;
import ru.vm5277.compiler.semantic.Symbol;

class ReachableAnalyzer {

	// Ищем static FieldNode со включенным флагом reassigned, и выполняем кодогенерацию в нем(что скорее всего уже выполнено) и в его классе/интерфейсе
	public static void analyze(MessageContainer mc, ObjectTypeNode objectTypeNode, CodeGenerator cg, CGExcs excs) {
		AstNode classBlockNode = objectTypeNode.getBody();
		if(null!=classBlockNode) {
			for(AstNode node : classBlockNode.getChildren()) {
				if(node instanceof FieldNode) {
					FieldNode fNode = (FieldNode)node;
					if(fNode.isStatic()) {
						try {
							Symbol symbol = fNode.getSymbol();
							if(null!=symbol && symbol.isReassigned()) {
								
								fNode.getObjectTypeNode().codeGen(cg, cg, false, excs);
								fNode.codeGen(cg, null, false, excs);
								//TODO пока статика работает не корректно cg.terminate(fNode.getCGScope(), false, true);
							}
						}
						catch(CompileException ex) {
							mc.add(ex.getErrorMessage());
						}
					}
				}
				else if(node instanceof ObjectTypeNode) {
					analyze(mc, (ObjectTypeNode)node, cg, excs);
				}
			}
		}
	}
}
