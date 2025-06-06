/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
30.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.nodes;

import ru.vm5277.avr_asm.TokenBuffer;
import ru.vm5277.avr_asm.scope.Scope;
import ru.vm5277.avr_asm.semantic.Expression;
import ru.vm5277.avr_asm.semantic.LiteralExpression;
import ru.vm5277.avr_asm.semantic.VariableExpression;
import ru.vm5277.common.Delimiter;
import ru.vm5277.common.Operator;
import ru.vm5277.common.SourcePosition;
import ru.vm5277.common.TokenType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.messages.ErrorMessage;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.common.tokens.Token;

public class Node {
	protected			TokenBuffer				tb;
	protected			MessageContainer		mc;
	protected			Scope					scope;
	protected			SourcePosition			sp;
	private				ErrorMessage			error;
	
	protected Node() {
	}
	
	protected Node(TokenBuffer tb, Scope scope, MessageContainer mc) {
        this.tb = tb;
		this.sp = null == tb ? null : tb.current().getSP();
		this.scope = scope;
		this.mc = mc;
    }
	
	
	public static Token consumeToken(TokenBuffer tb, TokenType expectedType) throws ParseException {
		if (tb.current().getType() == expectedType) return tb.consume();
		else throw new ParseException("Expected " + expectedType + ", but got " + tb.current().getType(), tb.getSP());
    }
	public static Token consumeToken(TokenBuffer tb, Delimiter delimiter) throws ParseException {
		if (TokenType.DELIMITER == tb.current().getType()) {
            if(delimiter == tb.current().getValue()) return tb.consume();
			else throw new ParseException("Expected delimiter " + delimiter + ", but got " + tb.current().getValue(), tb.getSP());
        }
		else throw new ParseException("Expected " + TokenType.DELIMITER + ", but got " + tb.current().getType(), tb.getSP());
    }
	public static Token consumeToken(TokenBuffer tb, Operator operator) throws ParseException {
		if (TokenType.OPERATOR == tb.current().getType()) {
            if(operator == tb.current().getValue()) return tb.consume();
			else throw new ParseException("Expected operator " + operator + ", but got " + tb.current().getValue(), tb.getSP());
        }
		else throw new ParseException("Expected " + TokenType.OPERATOR + ", but got " + tb.current().getType(), tb.getSP());
    }

	public WarningMessage markWarning(String text) {
		WarningMessage message = new WarningMessage(text, sp);
		mc.add(message);
		return message;
	}
	public WarningMessage markWarning(String text, SourcePosition sp) {
		WarningMessage message = new WarningMessage(text, sp);
		mc.add(message);
		return message;
	}

	public ErrorMessage markError(ParseException e) {
		ErrorMessage message = new ErrorMessage(e.getMessage(), sp);
		if(null == error) error = message;
		mc.add(message);
		return message;
	}
	public ErrorMessage markError(String text) {
		ErrorMessage message = new ErrorMessage(text, sp);
		if(null == error) error = message;
		mc.add(message);
		return message;
	}

	public static Long getValue(Expression expr, SourcePosition sp) throws ParseException {
		if(expr instanceof VariableExpression) {
			return ((VariableExpression)expr).getValue();
		}
		if(expr instanceof LiteralExpression) {
			return getLong(((LiteralExpression)expr).getValue());
		}
		throw new ParseException("TODO не поддерживаемое выражение:" + expr, sp);
	}

	private static long getLong(Object obj) throws ParseException {
		if(obj instanceof Integer) {
			return ((Integer)obj).longValue();
		}
		if(obj instanceof Long) {
			return (Long)obj;
		}
		if(obj instanceof Boolean) {
			return ((Boolean)obj) ? 0x01l : 0x00l;
		}
		throw new ParseException("TODO не поддерживаемый тип значения:" + obj, null);
	}
	
	public Scope getScope() {
		return scope;
	}
}
