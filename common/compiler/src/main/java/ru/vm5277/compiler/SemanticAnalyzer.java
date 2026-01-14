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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.lexer.J8BKeyword;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.ObjectTypeNode;
import ru.vm5277.compiler.semantic.GlobalScope;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.Keyword;

public class SemanticAnalyzer {
	protected SemanticAnalyzer() {
	}

	public static void analyze(ObjectTypeNode clazz, CodeGenerator cg, List<ObjectTypeNode> autoImported) {
		GlobalScope gScope = new GlobalScope();

		if(null!=autoImported) {
			for(ObjectTypeNode node : autoImported) {
				if(node.preAnalyze()) {
					if(node.declare(gScope)) {
						node.postAnalyze(gScope, cg);
					}
				}
			}
		}
		
		if(clazz.preAnalyze()) {
			if(clazz.declare(gScope)) {
				// Выполняем если все declare отработали и никаких отложенных.
				if(AstNode.getDeclarationPendingNodes().isEmpty()) {
					if(clazz.postAnalyze(gScope, cg)) {
						if(Optimization.NONE!=Main.getOptLevel()) {
							clazz.codeOptimization(gScope, cg);
						}
					}
				}
			}
		}
		
		// Пытаемся выполнить отложенные
		if(!AstNode.getDeclarationPendingNodes().isEmpty()) {
			debugAST(null, DECLARE, true, "---Try to resolve declaration pending nodes---");
			
			boolean result = true;
			int pendingsQnt = AstNode.getDeclarationPendingNodes().size();
			while(0!=pendingsQnt) {
				for(AstNode declarationPendingNode : new ArrayList<AstNode>(AstNode.getDeclarationPendingNodes().keySet())) {
					Scope scope = AstNode.getDeclarationPendingNodes().get(declarationPendingNode);
					// Ноды могут вызвать declare у вложенных, поэтому можем получить null
					if(null!=scope) {
						result&=declarationPendingNode.declare(scope);
					}
				}
				// Никаких изменений? Значит повторять нет смысла
				if(pendingsQnt==AstNode.getDeclarationPendingNodes().size()) break;
				pendingsQnt = AstNode.getDeclarationPendingNodes().size();
			}

			if(AstNode.getDeclarationPendingNodes().isEmpty()) {
				// Все отложенные выполнены и ошибок не было
				if(result) {
					if(clazz.postAnalyze(gScope, cg)) {
						if(Optimization.NONE!=Main.getOptLevel()) {
							clazz.codeOptimization(gScope, cg);
						}
					}
				}
			}
			else {
				// Остались отложенные, выдаем ошибки
				for(AstNode pendingNode : AstNode.getDeclarationPendingNodes().keySet()) {
					pendingNode.declare(AstNode.getDeclarationPendingNodes().get(pendingNode));
					pendingNode.markError("Unresolved forward reference: " + pendingNode);
				}
			}
		}
	}
	
	public boolean preAnalyze() {return false;}
	public boolean declare(Scope scope)  {return false;}
	public boolean postAnalyze(Scope scope, CodeGenerator cg)  {return false;}
	public void codeOptimization(Scope scope, CodeGenerator cg) {};
	
	public void validateName(String name) throws CompileException {
        if(name==null || name.isEmpty()) throw new CompileException("Name cannot be empty");
        if(null!=J8BKeyword.fromString(name)) throw new CompileException("Name cannot be a J8BKeyword");
    }
	
	protected void validateModifiers(Set<Keyword> modifiers, Keyword... allowedModifiers) throws CompileException {
		if(modifiers.contains(J8BKeyword.PUBLIC) && modifiers.contains(J8BKeyword.PRIVATE)) {
			throw new CompileException("Conflicting access modifiers: cannot combine 'public' and 'private'");
		}

		// Создаем Set из разрешенных модификаторов
		Set<Keyword> allowedSet = new HashSet<>(Arrays.asList(allowedModifiers));
		boolean hasInvalid = false;
		StringBuilder invalidMods = new StringBuilder();

		// Проверяем каждый модификатор класса
		for (Keyword mod : modifiers) {
			if (!allowedSet.contains(mod)) {
				if (hasInvalid) invalidMods.append(", ");
				invalidMods.append(mod);
				hasInvalid = true;
			}
		}

		// Если есть недопустимые модификаторы
		if (hasInvalid) throw new CompileException("Invalid modifier(s): " + invalidMods.toString());
	}
	
	protected MethodScope findEnclosingMethodScope(Scope scope) {
		while (scope != null) {
			if (scope instanceof MethodScope) {
				return (MethodScope) scope;
			}
			scope = scope.getParent();
		}
		return null;
	}
}
