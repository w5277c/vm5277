/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
23.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b_compiler.nodes.commands;

import ru.vm5277.common.j8b_compiler.CodeGenerator;
import ru.vm5277.j8b_compiler.nodes.TokenBuffer;
import ru.vm5277.j8b_compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.j8b_compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.j8b_compiler.semantic.MethodScope;
import ru.vm5277.j8b_compiler.semantic.Scope;

public class ReturnNode extends CommandNode {
	private	ExpressionNode	expression;
	
	public ReturnNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
		
		consumeToken(tb); // Потребляем "return"
		
		try {this.expression = tb.match(Delimiter.SEMICOLON) ? null : new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
        
        // Обязательно потребляем точку с запятой
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
    }

	public ReturnNode(MessageContainer mc, ExpressionNode expression) {
		super(null, mc);
		
		this.expression = expression;
	}
	
    public ExpressionNode getExpression() {
        return expression;
    }

    public boolean returnsValue() {
        return null != expression;
    }
	
	@Override
	public String getNodeType() {
		return "return command";
	}

	@Override
	public boolean preAnalyze() {
		if (expression != null) expression.preAnalyze();
		
		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Находим ближайший MethodScope
		MethodScope methodScope = findEnclosingMethodScope(scope);
		if (null == methodScope) {
			markError("'return' outside of method");
		}
		else {
			// Получаем тип возвращаемого значения метода
			VarType methodReturnType = methodScope.getSymbol().getType();

			// Проверяем соответствие возвращаемого значения
			if (null == expression) { // return без значения
				if (!methodReturnType.isVoid()) markError("Void method cannot return a value");
			}
			else { // return с выражением
				if (methodReturnType.isVoid()) markError("Non-void method must return a value");
				else {
					// Проверяем тип выражения
					try {
						VarType exprType = expression.getType(scope);
						if (!isCompatibleWith(scope, exprType, methodReturnType)) {
							markError(String.format("Return type mismatch: expected " + methodReturnType + ", got " + exprType));
						}
						
						// Дополнительная проверка на сужающее преобразование
						if (exprType.isNumeric() && methodReturnType.isNumeric() && exprType.getSize() < methodReturnType.getSize()) {
							markError("Narrowing conversion from " + methodReturnType + " to " + exprType + " requires explicit cast");
						}

					}
					catch (SemanticException e) {markError(e);}
				}
			}
		}
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		if(null != expression) {
			expression.codeGen(cg);
		}
		cg.eReturn();
	}
}
