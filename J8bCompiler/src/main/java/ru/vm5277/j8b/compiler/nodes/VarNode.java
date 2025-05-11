/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
10.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.messages.WarningMessage;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.semantic.BlockScope;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class VarNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private	final	VarType			type;
	private	final	String			name;
	private			ExpressionNode	initializer;

	public VarNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            initializer = null;
        }
		else {
			consumeToken(tb);
			try {initializer = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	public VarNode(MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(null, mc);
		
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return initializer;
	}
	
	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}

	@Override
	public String getNodeType() {
		return "var";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + type + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		if(Character.isUpperCase(name.charAt(0))) addMessage(new WarningMessage("Variable name should start with lowercase letter:" + name, sp));

		return true;
	}

	
	@Override
	public boolean declare(Scope scope) {
		if(scope instanceof BlockScope) {
			BlockScope blockScope = (BlockScope)scope;

			try {blockScope.addLocal(new Symbol(name, type, modifiers.contains(Keyword.FINAL)));} catch(SemanticException e) {markError(e);}
		}
		else markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in var:" + name);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка инициализации final-полей
		if (isFinal() && initializer == null) markError("Final variable  '" + name + "' must be initialized");

		// Анализ инициализатора, если есть
		if (initializer != null) initializer.postAnalyze(scope);

		// Проверка совместимости типов
		try {
			VarType initType = initializer.getType(scope);
			if (!type.isCompatibleWith(initType)) {
				markError("Type mismatch: cannot assign " + initType + " to " + type);
			}
		}
		catch (SemanticException e) {markError(e);}
		
		return true;
	}
}