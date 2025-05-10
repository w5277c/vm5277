/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.nodes.ClassNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.MethodScope;
import ru.vm5277.j8b.compiler.semantic.Scope;

public abstract class SemanticAnalyzer {
	protected			TokenBuffer	tb;
	private				Scope		scope;
	
	protected SemanticAnalyzer() {
	}

	public SemanticAnalyzer(ClassNode clazz) throws SemanticException {
		scope = new ClassScope(null, null);
		
		if(clazz.preAnalyze()) {
			if(clazz.declare(scope)) {
				clazz.postAnalyze(scope);
			}
		}
	}

	public abstract boolean preAnalyze();
	public abstract boolean declare(Scope scope);
	public abstract boolean postAnalyze(Scope scope);
	
	
	public void validateName(String name) throws SemanticException {
        if (name == null || name.isEmpty()) throw tb.semanticError("Name cannot be empty");
        if (null != Keyword.fromString(name)) throw tb.semanticError("Name cannot be a keyword");
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
	
	public Scope getGlobalScope() {
		return scope;
	}
}
