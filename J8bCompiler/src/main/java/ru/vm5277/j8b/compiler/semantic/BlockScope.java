/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
07.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.semantic;

import java.util.HashMap;
import java.util.Map;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;

public class BlockScope implements Scope {
	private	final	Scope						parent;
	private	final	Map<String, Symbol>			locals	= new HashMap<>();
	private	final	Map<String, LabelSymbol>	labels	= new HashMap<>();
	
	public BlockScope(Scope parent) {
		this.parent = parent;
	}

	public void addLocal(Symbol symbol) throws SemanticException {
		String name = symbol.getName();
		if (locals.containsKey(name)) throw new SemanticException("Duplicate local variable: " + name);
		locals.put(name, symbol);
	}

	@Override
	public Symbol resolve(String name) {
		// 1. Ищем в локальных переменных
		Symbol symbol = locals.get(name);
		if (symbol != null) return symbol;

		// 2. Делегируем в родительскую область
		return parent != null ? parent.resolve(name) : null;
	}
	
	public void addLabel(LabelSymbol label) throws SemanticException {
		String name = label.getName();
		if (labels.containsKey(name)) throw new SemanticException("Duplicate label: " + name);
		labels.put(name, label);
	}

	public LabelSymbol resolveLabel(String name) {
		// Ищем метку в текущей и родительских областях
		BlockScope current = this;
		while (current != null) {
			LabelSymbol symbol = current.labels.get(name);
			if(null != symbol) return symbol;
			current = (current.getParent() instanceof BlockScope) ? (BlockScope)current.getParent() : null;
		}
		return null;
	}
	
	@Override
	public Scope getParent() {
		return parent;
	}

	@Override
	public ClassScope resolveClass(String className) {
		return Scope.resolveClass(this, className);
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
