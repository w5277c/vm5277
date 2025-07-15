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

import java.util.List;
import ru.vm5277.common.messages.InfoMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ClassBlockNode;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.nodes.MethodNode;
import ru.vm5277.compiler.nodes.expressions.FieldAccessExpression;
import ru.vm5277.compiler.nodes.expressions.VarFieldExpression;
import ru.vm5277.compiler.semantic.FieldSymbol;
import ru.vm5277.compiler.semantic.VarSymbol;

class ReachableAnalyzer {
	public static void analyze(ClassNode clazz, String launchMethod, MessageContainer mc) {
		ClassBlockNode classBlockNode = clazz.getBody();
		for(AstNode node : classBlockNode.getChildren()) {
			if(node instanceof MethodNode) {
				MethodNode mNode = (MethodNode)node;
				if(mNode.isPublic() && mNode.isStatic() && mNode.getParameters().isEmpty() && mNode.getName().equals(launchMethod)) {
					//mNode.setUsed();
					visit(mNode, mc, false);
					break;
				}
			}
		}
	}
	
	private static void visit(AstNode parentNode, MessageContainer mc, boolean mustUsed) {
/*		//TODO статические блоки и методы должны быть с включенным флагом isUsed
		if(null != parentNode.getChildren()) {
			for(AstNode node : parentNode.getChildren()) {
				visit(node, mc, mustUsed);
			}
		}

		if(mustUsed || parentNode instanceof FieldAccessExpression || parentNode instanceof VarExpression) {
			Boolean isUsed = parentNode.isUsed();
			if(null != isUsed && !isUsed.booleanValue()) {
				parentNode.setUsed();
				mc.add(new InfoMessage("Mark as used: " + parentNode.getSymbol(), null));
				if(parentNode.getSymbol() instanceof FieldSymbol) {
					List<AstNode> fieldNodes = ((FieldSymbol)parentNode.getSymbol()).getFieldNode().getChildren();
					if(null != fieldNodes) {
						for(AstNode node : fieldNodes) {
							visit(node, mc, true);
						}
					}
				}
				if(parentNode.getSymbol() instanceof VarSymbol) {
					List<AstNode> varNodes = ((VarSymbol)parentNode.getSymbol()).getVarNode().getChildren();
					if(null != varNodes) {
						for(AstNode node : varNodes) {
							visit(node, mc, true);
						}
					}
				}
			}
		}*/
	}
}
