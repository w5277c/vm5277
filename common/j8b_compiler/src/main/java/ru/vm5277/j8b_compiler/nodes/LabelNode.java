/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes;

import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.j8b_compiler.semantic.BlockScope;
import ru.vm5277.j8b_compiler.semantic.LabelSymbol;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class LabelNode extends AstNode {
	private	final	String	name;
	private			boolean	used	= false;

	public LabelNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		this.name = consumeToken(tb).getStringValue(); // Гарантирован вызывающим
	}

	public String getName() {
		return name;
	}

	public void setUsed() {
		used = true;
	}
	public boolean isUsed() {
		return used;
	}

	@Override
	public String getNodeType() {
		return "label";
	}

	@Override
	public boolean preAnalyze() {
		return true;
	}
	@Override
	public boolean declare(Scope scope) {
		if (!(scope instanceof BlockScope)) markError("Labels can only be declared in block scope");
        
		try {((BlockScope)scope).addLabel(new LabelSymbol(name, scope));} catch (SemanticException e) {markError(e);}

		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope) {
		if (!used) addMessage(new WarningMessage("Unused label '" + name + "'", sp));

		// TODO Контроль достижимости кода после return/break/continue
		return true;
	}

	@Override
	public String toString() {
		return "label: " + name;
	}
}