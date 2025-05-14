/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes;

import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import java.util.Set;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.messages.WarningMessage;
import ru.vm5277.j8b.compiler.semantic.ClassScope;
import ru.vm5277.j8b.compiler.semantic.Scope;
import ru.vm5277.j8b.compiler.semantic.Symbol;

public class FieldNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private			VarType			returnType;
	private			String			name;
	private			ExpressionNode	initializer;

	public FieldNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType returnType, String name) {
		super(tb, mc);

		this.modifiers = modifiers;
		this.returnType = returnType;
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

	public FieldNode(MessageContainer mc, Set<Keyword> modifiers, VarType returnType, String name) {
		super(null, mc);
		
		this.modifiers = modifiers;
		this.returnType = returnType;
		this.name = name;
	}
	
	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	public VarType getType() {
		return returnType;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return initializer;
	}
	
	public boolean isStatic() {
		return modifiers.contains(Keyword.STATIC);
	}
	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}
	public boolean isPublic() {
		return modifiers.contains(Keyword.PUBLIC);
	}

	@Override
	public String getNodeType() {
		return "field";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + returnType + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		if(Character.isUpperCase(name.charAt(0))) addMessage(new WarningMessage("Field name should start with lowercase letter:" + name, sp));

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if(scope instanceof ClassScope) {
			ClassScope classScope = (ClassScope)scope;

			try{classScope.addField(new Symbol(name, returnType, modifiers.contains(Keyword.FINAL), modifiers.contains(Keyword.STATIC)));}
			catch(SemanticException e) {markError(e);}
		}
		else markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in filed:" + name);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка инициализации final-полей
		if (isFinal() && initializer == null) markError("Final field '" + name + "' must be initialized");

		// Анализ инициализатора, если есть
		if (initializer != null) initializer.postAnalyze(scope);

		// Проверка совместимости типов
		try {
			VarType initType = initializer.getType(scope);
			if (!returnType.isCompatibleWith(initType)) {
				markError("Type mismatch: cannot assign " + initType + " to " + returnType);
			}
			// Дополнительная проверка на сужающее преобразование
			if (returnType.isNumeric() && initType.isNumeric() && returnType.getSize() < initType.getSize()) {
				markError("Narrowing conversion from " + initType + " to " + returnType + " requires explicit cast");
			}
		}
		catch (SemanticException e) {markError(e);}
		
		return true;
	}
}
