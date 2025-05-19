/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
12.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.expressions;

import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;

public class MemberAccessExpression extends ExpressionNode {
	private	final	ExpressionNode	target;
	private	final	String			memberName;

	public MemberAccessExpression(TokenBuffer tb, MessageContainer mc, ExpressionNode target, String memberName) {
		super(tb, mc);
		this.target = target;
		this.memberName = memberName;
	}

	public ExpressionNode getTarget() {
		return target;
	}

	public String getMemberName() {
		return memberName;
	}

	@Override
	public String getNodeType() {
		return "member access";
	}

	// Реализация остальных методов (getType, analyze и т.д.)
}