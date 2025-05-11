/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
28.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.nodes.commands;

import ru.vm5277.j8b.compiler.nodes.*;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.enums.Delimiter;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.j8b.compiler.semantic.BlockScope;
import ru.vm5277.j8b.compiler.semantic.Scope;

public class ForNode extends CommandNode {
    private	AstNode			initialization;
    private	ExpressionNode	condition;
    private	ExpressionNode	iteration;
    private	BlockScope		forScope;
	private	BlockScope		bodyScope;
	private	BlockScope		elseScope;
	
    public ForNode(TokenBuffer tb, MessageContainer mc) {
        super(tb, mc);
        
        consumeToken(tb); // Потребляем "for"
        try {consumeToken(tb, Delimiter.LEFT_PAREN);} catch(ParseException e) {markFirstError(e);}
        
        // Инициализация
        if(!tb.match(Delimiter.SEMICOLON)) {
			try {
				VarType type = checkPrimtiveType();
				if(null == type) type = checkClassType();
				if(null != type) {
					String name = null;
					try {name = consumeToken(tb, TokenType.ID).getStringValue();} catch(ParseException e) {markFirstError(e);}
					this.initialization = new VarNode(tb, mc, null, type, name);
				}
				else {
					this.initialization = new ExpressionNode(tb, mc).parse();
				}
			}
			catch(ParseException e) {
				markFirstError(e);
			}
		}
		else {
			this.initialization = null;
			try {consumeToken(tb, Delimiter.SEMICOLON);} catch(ParseException e) {markFirstError(e);}
		}
        
        // Условие
        try {this.condition = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
        try {consumeToken(tb, Delimiter.SEMICOLON);} catch(ParseException e) {markFirstError(e);}
        
        // Итерация
        try {this.iteration = tb.match(Delimiter.RIGHT_PAREN) ? null : new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
        try {consumeToken(tb, Delimiter.SEMICOLON);} catch(ParseException e) {markFirstError(e);}
        
		try {consumeToken(tb, Delimiter.RIGHT_PAREN);} catch(ParseException e) {markFirstError(e);}
		
        // Основной блок
		tb.getLoopStack().add(this);
		try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
		catch(ParseException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);
       
        // Блок else (если есть)
        if (tb.match(Keyword.ELSE)) {
			consumeToken(tb);
			tb.getLoopStack().add(this);
			try {blocks.add(tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement()));}
			catch(ParseException e) {markFirstError(e);}
			tb.getLoopStack().remove(this);
        }
    }
    
    // Геттеры
    public AstNode getInitialization() {
        return initialization;
    }
    
    public ExpressionNode getCondition() {
        return condition;
    }
    
    public ExpressionNode getIteration() {
        return iteration;
    }
    
    public BlockNode getBody() {
        return blocks.isEmpty() ? null : blocks.get(0);
    }
    
    public BlockNode getElseBlock() {
        return blocks.size() > 1 ? blocks.get(1) : null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("for (");
        sb.append(initialization != null ? initialization : ";");
        sb.append(condition != null ? condition : ";");
        sb.append(iteration != null ? iteration : "");
        sb.append(") ");
        sb.append(getBody());
        
        if (getElseBlock() != null) {
            sb.append(" else ").append(getElseBlock());
        }
        
        return sb.toString();
    }

	@Override
	public String getNodeType() {
		return "for loop";
	}

	@Override
	public boolean preAnalyze() {
		// Проверка блока инициализации
		if (null != initialization) initialization.preAnalyze();

		// Проверка условия цикла
		if (null != condition) condition.preAnalyze();

		// Проверка блока итерации
		if (null != iteration) iteration.preAnalyze();

		// Проверка основного тела цикла
		if (null != getBody()) getBody().preAnalyze();

		// Проверка else-блока (если есть)
		if (null != getElseBlock()) getElseBlock().preAnalyze();
		
		return  true;
	}

	@Override
	public boolean declare(Scope scope) {
		// Создаем новую область видимости для всего цикла
		forScope = new BlockScope(scope);

		// Объявление блока инициализации
		if (null != initialization) initialization.declare(forScope);

		// Объявление условия
		if (null != condition) condition.declare(forScope);

		// Объявление блока итерации
		if (null != iteration) iteration.declare(forScope);

		// Объявление тела цикла
		if (null != getBody()) {
			bodyScope = new BlockScope(forScope);
			getBody().declare(bodyScope);
		}
		
		// Объявление else-блока (если есть)
		if (null != getElseBlock()) {
			elseScope = new BlockScope(forScope);
			getElseBlock().declare(elseScope);
		}
		
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Анализ блока инициализации
		if (initialization != null) initialization.postAnalyze(forScope);

		// Проверка типа условия
		if (condition != null) {
			if (condition.postAnalyze(forScope)) {
				try {
					VarType condType = condition.getType(forScope);
					if (VarType.BOOL != condType && condType != null) markError("For loop condition must be boolean, got: " + condType);
				}
				catch (SemanticException e) {markError(e);}
			}
		}

		// Анализ блока итерации
		if (null != iteration) iteration.postAnalyze(forScope);

		// Анализ тела цикла
		if (null != getBody()) {
			getBody().postAnalyze(bodyScope);
		}
		
		// Анализ else-блока
		if (null != getElseBlock()) {
			getElseBlock().postAnalyze(elseScope);
		}
		
		// Проверяем бесконечный цикл с возвратом
		if (condition instanceof LiteralExpression && Boolean.TRUE.equals(((LiteralExpression)condition).getValue()) &&	isControlFlowInterrupted(getBody())) {
			markWarning("Code after infinite while loop is unreachable");
		}

		return true;
	}
}