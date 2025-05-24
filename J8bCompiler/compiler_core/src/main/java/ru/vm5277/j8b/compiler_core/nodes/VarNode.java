/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
10.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler_core.nodes;

import java.util.Set;
import ru.vm5277.j8b.compiler.common.CodeGenerator;
import ru.vm5277.j8b.compiler.common.Operand;
import ru.vm5277.j8b.compiler.common.enums.OperandType;
import ru.vm5277.j8b.compiler_core.enums.Delimiter;
import ru.vm5277.j8b.compiler_core.enums.Keyword;
import ru.vm5277.j8b.compiler.common.enums.Operator;
import ru.vm5277.j8b.compiler.common.enums.VarType;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;
import ru.vm5277.j8b.compiler.common.messages.WarningMessage;
import ru.vm5277.j8b.compiler_core.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler_core.nodes.expressions.LiteralExpression;
import ru.vm5277.j8b.compiler_core.nodes.expressions.VariableExpression;
import ru.vm5277.j8b.compiler_core.semantic.BlockScope;
import ru.vm5277.j8b.compiler_core.semantic.Scope;
import ru.vm5277.j8b.compiler_core.semantic.Symbol;

public class VarNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private	final	VarType			type;
	private	final	String			name;
	private			ExpressionNode	initializer;
	private			Symbol			symbol;
	
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

	public Set<Keyword> getModifiers() {
		return modifiers;
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
			boolean isFinal = modifiers.contains(Keyword.FINAL);
			symbol = new Symbol(name, type, isFinal, modifiers.contains(Keyword.STATIC));
			if(isFinal && null != initializer) {
				if(initializer instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)initializer;
					symbol.setConstantOperand(new Operand(le.getType(scope).getId(), OperandType.LITERAL, le.getValue()));
				}
				else if(initializer instanceof VariableExpression) {
					VariableExpression ve = (VariableExpression)initializer;
					symbol.setConstantOperand(new Operand(ve.getType(scope).getId(), OperandType.TYPE, ve.getValue()));
				}
			}

			try {blockScope.addLocal(symbol);}
			catch(SemanticException e) {markError(e);}
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
			if (!isCompatibleWith(scope, type, initType)) {
				markError("Type mismatch: cannot assign " + initType + " to " + type);
			}
			
			// Дополнительная проверка на сужающее преобразование
			if (type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) { //TODO верятно нужно и в других местах
				markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast"); 
			}
		}
		catch (SemanticException e) {markError(e);}
		
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) {
		symbol.setRuntimeId(cg.enterFiled(type.getId(), name));
		try {
			initializer.codeGen(cg);
		}
		finally {
			cg.leave();
		}
	}
}