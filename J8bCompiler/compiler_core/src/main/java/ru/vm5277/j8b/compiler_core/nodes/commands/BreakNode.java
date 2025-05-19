/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes.commands;

import ru.vm5277.j8b.compiler_core.nodes.AstNode;
import ru.vm5277.j8b.compiler_core.nodes.TokenBuffer;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.TokenType;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler_core.semantic.BlockScope;
import ru.vm5277.j8b.compiler_core.semantic.LabelSymbol;
import ru.vm5277.j8b.compiler_core.semantic.Scope;

public class BreakNode extends CommandNode {
    private	String		label;
	private LabelSymbol symbol;
	
	public BreakNode(TokenBuffer tb, MessageContainer mc) {
        super(tb, mc);
        
        consumeToken(tb);
        
		if(tb.match(TokenType.ID)) {
			try {label = consumeToken(tb, TokenType.ID).toString();}catch(ParseException e) {markFirstError(e);};
		}
		
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
        
		AstNode node = tb.getLoopStack().peek();
		if (null == node || !(node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode)) {
			markFirstError(parserError("'break' can only be used inside loop statements"));
        }
    }

	public String getLabel() {
		return label;
	}

	@Override
	public String getNodeType() {
		return "break command";
	}
	
	@Override
    public String toString() {
        return "break" + (null != label ? " " + label : "");
    }

	@Override
	public boolean preAnalyze() {
		// Проверка наличия метки (если указана)
		if (label == null) markError("Break label cannot be empty");

		// Базовая проверка - команда break должна быть внутри цикла
        if (tb.getLoopStack().isEmpty()) markError("'break' outside of loop");

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
		// Проверка для break с меткой
        if (null != symbol) {

			// Метка должна быть на цикле или switch
            if (!isLoopOrSwitch((CommandNode)tb.getLoopStack().peek())) markError("Break label must be on loop or switch statement");
            
            // Проверка видимости
            if (!isLabelInCurrentMethod(symbol, scope)) markError("Cannot break to label in different method");
        }
		return true;
	}
}