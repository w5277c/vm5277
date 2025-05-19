/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.common.messages.MessageOwner;
import ru.vm5277.j8b.compiler_core.nodes.ClassNode;
import ru.vm5277.j8b.compiler_core.semantic.ClassScope;
import ru.vm5277.j8b.compiler_core.semantic.InterfaceSymbol;
import ru.vm5277.j8b.compiler_core.semantic.MethodScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class SemanticAnalyzer {
	private	ClassScope	globalScope;
	
	protected SemanticAnalyzer() {
	}

	public SemanticAnalyzer(String runtimePath, ClassNode clazz) throws SemanticException {
		clazz.getMessageContainer().setOwner(MessageOwner.SEMANTIC);
		globalScope = new ClassScope();
		
		//TODO runtime
		globalScope.addInterface(new InterfaceSymbol("Object"));
		
		if(clazz.preAnalyze()) {
			if(clazz.declare(globalScope)) {
				clazz.postAnalyze(globalScope);
			}
		}
	}

	public boolean preAnalyze() {return false;}
	public boolean declare(Scope scope)  {return false;}
	public boolean postAnalyze(Scope scope)  {return false;}
	
	
	public void validateName(String name) throws SemanticException {
        if (name == null || name.isEmpty()) throw new SemanticException("Name cannot be empty");
        if (null != Keyword.fromString(name)) throw new SemanticException("Name cannot be a keyword");
    }
	
	protected void validateModifiers(Set<Keyword> modifiers, Keyword... allowedModifiers) throws SemanticException {
		if(modifiers.contains(Keyword.PUBLIC) && modifiers.contains(Keyword.PRIVATE)) {
			throw new SemanticException("Conflicting access modifiers: cannot combine 'public' and 'private'");
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
		if (hasInvalid) throw new SemanticException("Invalid modifier(s): " + invalidMods.toString());
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
	
	public ClassScope getGlobalScope() {
		return globalScope;
	}
}
