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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.ClassNode;
import ru.vm5277.compiler.semantic.ClassScope;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;

public class SemanticAnalyzer {
	protected SemanticAnalyzer() {
	}

	public static void analyze(ClassScope globalScope, ClassNode clazz, CodeGenerator cg) {
		if(clazz.preAnalyze()) {
			if(clazz.declare(globalScope)) {
				clazz.postAnalyze(globalScope, cg);
			}
		}
	}
	
	
	public boolean preAnalyze() {return false;}
	public boolean declare(Scope scope)  {return false;}
	public boolean postAnalyze(Scope scope, CodeGenerator cg)  {return false;}
	
	
	public void validateName(String name) throws CompileException {
        if (name == null || name.isEmpty()) throw new CompileException("Name cannot be empty");
        if (null != Keyword.fromString(name)) throw new CompileException("Name cannot be a keyword");
    }
	
	protected void validateModifiers(Set<Keyword> modifiers, Keyword... allowedModifiers) throws CompileException {
		if(modifiers.contains(Keyword.PUBLIC) && modifiers.contains(Keyword.PRIVATE)) {
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
