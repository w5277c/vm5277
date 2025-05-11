/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
01.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class FreeNode extends AstNode {
	private ExpressionNode target;

	public FreeNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);

		consumeToken(tb); // Наличие гарантировано вызывающим
		try {this.target = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	@Override
	public boolean preAnalyze() {
		if (target == null) markError("Invalid free expression");

		return true;
	}
	@Override
	public boolean declare(Scope scope) {
        return true;
	}
	@Override
	public boolean postAnalyze(Scope scope) {
		if (null != target && target.postAnalyze(scope)) {
			try {
				VarType type = target.getType(scope);
				if (!type.isReferenceType() || type == VarType.CSTR) {
					markError("Free can only be applied to pointers, got: " + type);
				}
			}
			catch (SemanticException e) {markError(e);}
		}
		return true;
	}
	
	@Override
	public String getNodeType() {
		return "free";
	}
	
	@Override
	public String toString() {
		return "free: " + target;
	}
}