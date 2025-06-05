/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.commands;

import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.j8b_compiler.nodes.BlockNode;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.J8bKeyword;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.nodes.expressions.InstanceOfExpression;
import ru.vm5277.j8b_compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.j8b_compiler.semantic.BlockScope;
import ru.vm5277.j8b_compiler.semantic.Scope;
import ru.vm5277.j8b_compiler.semantic.Symbol;

public class IfNode extends CommandNode {
    private	ExpressionNode	condition;
	private	BlockScope		thenScope;
	private	BlockScope		elseScope;
	private	String			varName;
	private	boolean			alwaysTrue;
	private	boolean			alwaysFalse;
	
	public IfNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
        consumeToken(tb); // Потребляем "if"
		// Условие
		try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e){markFirstError(e);}
		try {this.condition = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		// Парсинг условия (обычное или pattern matching)
		if (tb.match(J8bKeyword.AS)) {
			tb.consume(); // Потребляем "as"
			// Проверяем, что после типа идет идентификатор
			if (tb.match(TokenType.ID)) {
				this.varName = consumeToken(tb).getStringValue();
			}
			else {
				markError("Expected variable name after type in pattern matching");
			}
		}
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e){markFirstError(e);}

		// Then блок
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);

		// Else блок
        if (tb.match(J8bKeyword.ELSE)) {
			consumeToken(tb);
        
			if (tb.match(TokenType.COMMAND, J8bKeyword.IF)) {
				// Обработка else if
				tb.getLoopStack().add(this);
				blocks.add(new BlockNode(tb, mc, new IfNode(tb, mc)));
				tb.getLoopStack().remove(this);
			}
			else {
				tb.getLoopStack().add(this);
				try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
				catch(ParseException e) {markFirstError(e);}
				tb.getLoopStack().remove(this);
			}
		}
	}

    // Геттеры
    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return blocks.get(0);
    }

    public BlockNode getElseBlock() {
        return (0x02 == blocks.size() ? blocks.get(1) : null);
    }
	
	public String getVarName() {
		return varName;
	}

	public boolean alwaysTrue() {
		return alwaysTrue;
	}
	public boolean alwaysFalse() {
		return alwaysFalse;
	}

	@Override
	public String getNodeType() {
		return "if condition";
	}

	@Override
	public boolean preAnalyze() {
		if (condition != null) condition.preAnalyze();
		else markError("If condition cannot be null");

		// Проверка что есть хотя бы один блок
		if (null == getThenBlock() && null == getElseBlock()) markError("If statement must have at least one block (then or else)");
		
		// Проверка then-блока
		if (null != getThenBlock()) getThenBlock().preAnalyze();

		// Проверка else-блока (если есть)
		if (null != getElseBlock()) getElseBlock().preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Объявление переменных условия
		if (null != condition) condition.declare(scope);
		
		// Объявление then-блока в новой области видимости
		if (null != getThenBlock()) {
			thenScope = new BlockScope(scope);
		}
		
		// Для pattern matching: объявляем новую переменную в then-блоке
		if (null != varName) {
			if(condition instanceof InstanceOfExpression) {
				InstanceOfExpression instanceOf = (InstanceOfExpression) condition;
				try {
					VarType type = instanceOf.getTypeExpr().getType(scope);
					if (null != type && null != thenScope) {
						thenScope.addLocal(new Symbol(varName, type, false, false));
					}
				}
				catch (SemanticException e) {markError(e);
				}
			}
			else {
				markError("Pattern matching requires 'is' check before 'as'");
			}
		}		

		if(null != thenScope) {
			getThenBlock().declare(thenScope);
		}

		// Объявление else-блока в новой области видимости
		if (null != getElseBlock()) {
			elseScope = new BlockScope(scope);
			getElseBlock().declare(elseScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка типа условия
		if (null != condition) {
			if (condition.postAnalyze(scope)) {
				try {
					VarType condType = condition.getType(scope);
					if (VarType.BOOL != condType) markError("If condition must be boolean, got: " + condType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}

		// Анализ then-блока
		if (null != getThenBlock()) getThenBlock().postAnalyze(thenScope);

		// Анализ else-блока
		if (null != getElseBlock()) getElseBlock().postAnalyze(elseScope);
		
		// Проверяем недостижимый код после if с возвратом во всех ветках
        if (null != getElseBlock() && isControlFlowInterrupted(getThenBlock()) && isControlFlowInterrupted(getElseBlock())) {
            markWarning("Code after if statement may be unreachable");
        }
		
		try {
			condition = condition.optimizeWithScope(scope);
			if(condition instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)condition;
				if(VarType.BOOL == le.getType(scope)) {
					if((boolean)le.getValue()) {
						alwaysTrue = true;
					}
					else {
						alwaysFalse = true;
					}
				}
			}
		}
		catch (ParseException e) {
			markFirstError(e);
		}
		
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		if(alwaysTrue) {
			cg.enterBlock();
			getThenBlock().codeGen(cg);
			cg.leave();
			return;
		}
		if(alwaysFalse()) {
			if (getElseBlock() != null) {
				cg.enterBlock();
				getElseBlock().codeGen(cg);
				cg.leave();
			}
			return;
		}
		
		int condBlockId = cg.enterBlock();
		condition.codeGen(cg);
		cg.leave();
		
		int thenBlockId = cg.enterBlock();
		getThenBlock().codeGen(cg);
		cg.leave();

		Integer elseBlockId = null;
		if(null != getElseBlock()) {
			elseBlockId = cg.enterBlock();
			getElseBlock().codeGen(cg);
			cg.leave();
		}
		
		cg.eIf(condBlockId, thenBlockId, elseBlockId);
	}
}
