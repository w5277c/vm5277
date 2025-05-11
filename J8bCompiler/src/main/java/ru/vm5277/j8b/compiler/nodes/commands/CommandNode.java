/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
05.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.enums.ReturnStatus;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.AstNode;
import ru.vm5277.j8b.compiler.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.semantic.LabelSymbol;
import ru.vm5277.j8b.compiler.semantic.MethodScope;
import ru.vm5277.j8b.compiler.semantic.Scope;

public abstract class CommandNode extends AstNode {
	public CommandNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
	}
	
	protected boolean isLoopOrSwitch(CommandNode node) {
		return null != node && (node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode || node instanceof SwitchNode);
	}
	
	
	protected boolean isLabelInCurrentMethod(LabelSymbol symbol, Scope scope) {
		// Ищем метод в текущей цепочке областей видимости
		Scope current = scope;
		while (null != current) {
			if (current instanceof MethodScope) break;
			current = current.getParent();
		}

		// Ищем метод, содержащий метку
		Scope labelScope = symbol.getScope();
		while (null != labelScope) {
			if (labelScope instanceof MethodScope) {
				return labelScope == current;
			}
			labelScope = labelScope.getParent();
		}

		return false;
	}

	// Анализирует наличие return в команде
	public ReturnStatus getReturnStatus() {
		return ReturnStatus.NEVER;
	}
}
