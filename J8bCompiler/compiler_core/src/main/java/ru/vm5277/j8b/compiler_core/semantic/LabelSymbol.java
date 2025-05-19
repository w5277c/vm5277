/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
06.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.semantic;

import java.util.ArrayList;
import java.util.List;
import ru.vm5277.j8b.compiler_core.nodes.commands.CommandNode;

public class LabelSymbol extends Symbol {
	private	Scope				scope;
	private List<CommandNode>	references	= new ArrayList<>();

	public LabelSymbol(String name, Scope scope) {
		super(name);
		this.scope = scope;
	}
	
	public Scope getScope() {
		return scope;
	}
	
	public void addReference(CommandNode node) {
		references.add(node);
	}

	public List<CommandNode> getReferences() {
		return references;
	}

	public boolean isUsed() {
		return !references.isEmpty();
	}
}