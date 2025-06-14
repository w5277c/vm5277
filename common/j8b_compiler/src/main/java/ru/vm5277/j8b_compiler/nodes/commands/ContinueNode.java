/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.commands;

import ru.vm5277.j8b_compiler.nodes.AstNode;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.j8b_compiler.Delimiter;
import ru.vm5277.j8b_compiler.TokenType;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.semantic.BlockScope;
import ru.vm5277.j8b_compiler.semantic.LabelSymbol;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class ContinueNode extends CommandNode {
	private String		label;
	private	LabelSymbol	symbol;
	
	public ContinueNode(TokenBuffer tb, MessageContainer mc) {
        super(tb, mc);
        
        consumeToken(tb);
		if(tb.match(TokenType.ID)) {
			try {label = consumeToken(tb, TokenType.ID).toString();}catch(ParseException e) {markFirstError(e);};
		}
		
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
    }

	public String getLabel() {
		return label;
	}
	
	@Override
	public String getNodeType() {
		return "continue command";
	}

	@Override
    public String toString() {
		return "continue";
	}

	@Override
	public boolean preAnalyze() {
		// Базовая проверка - команда break должна быть внутри цикла
        if (tb.getLoopStack().isEmpty()) markError("'continue' outside of loop");

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (label != null) {
			if (!(scope instanceof BlockScope)) markError("Labeled break must be inside block scope");
			else {
				// Разрешаем метку
				symbol = ((BlockScope)scope).resolveLabel(label);
				if (null == symbol) markError("Undefined label: " + label);

				// Регистрируем использование метки
				symbol.addReference(this);
			}
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		AstNode node = tb.getLoopStack().peek();
		if (null == node || !(node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode)) {
            markFirstError(parserError("'continue' can only be used inside loop statements"));
        }

		// Проверка видимости
		if(null != label && !isLabelInCurrentMethod(symbol, scope)) markError("Cannot break to label in different method");
		
		return true;
	}
}