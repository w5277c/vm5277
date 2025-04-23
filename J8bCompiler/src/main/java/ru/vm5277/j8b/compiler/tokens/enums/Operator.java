/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.tokens.enums;

import java.util.HashMap;
import java.util.Map;

public enum Operator {
	// Базовые арифметические
	PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), MOD("%"),
	// Сравнения
	EQ("=="), NEQ("!="), LT("<"), GT(">"), LTE("<="), GTE(">="),
	// Логические
	AND("&&"), OR("||"), NOT("!"),
	// Битовые
	BIT_AND("&"), BIT_OR("|"), BIT_XOR("^"), BIT_NOT("~"), SHL("<<"), SHR(">>"),
	// Присваивание (простое и составное)
	ASSIGN("="), PLUS_ASSIGN("+="), MINUS_ASSIGN("-="), MULT_ASSIGN("*="), DIV_ASSIGN("/="), MOD_ASSIGN("%="), AND_ASSIGN("&="), OR_ASSIGN("|="),
	XOR_ASSIGN("^="), SHL_ASSIGN("<<="), SHR_ASSIGN(">>=");

	public static final Map<Operator, Integer> PRECEDENCE = new HashMap<>();
	static {
	    // 1. Присваивание (низший приоритет)
		PRECEDENCE.put(Operator.ASSIGN, 1);
		PRECEDENCE.put(Operator.PLUS_ASSIGN, 1);
		PRECEDENCE.put(Operator.MINUS_ASSIGN, 1);
		PRECEDENCE.put(Operator.MULT_ASSIGN, 1);
		PRECEDENCE.put(Operator.DIV_ASSIGN, 1);
		PRECEDENCE.put(Operator.MOD_ASSIGN, 1);
		PRECEDENCE.put(Operator.AND_ASSIGN, 1);
		PRECEDENCE.put(Operator.OR_ASSIGN, 1);
		PRECEDENCE.put(Operator.XOR_ASSIGN, 1);
		PRECEDENCE.put(Operator.SHL_ASSIGN, 1);
		PRECEDENCE.put(Operator.SHR_ASSIGN, 1);
	    // 2. Логическое ИЛИ
		PRECEDENCE.put(Operator.OR, 2);
	    // 3. Логическое И
		PRECEDENCE.put(Operator.AND, 3);
	    // 4. Побитовое ИЛИ
	    PRECEDENCE.put(Operator.BIT_OR, 4);
	    // 5. Побитовое XOR
		PRECEDENCE.put(Operator.BIT_XOR, 5);
	    // 6. Побитовое И
		PRECEDENCE.put(Operator.BIT_AND, 6);
	    // 7. Равенство
		PRECEDENCE.put(Operator.EQ, 7);
		PRECEDENCE.put(Operator.NEQ, 7);
	    // 8. Сравнение
		PRECEDENCE.put(Operator.LT, 8);
		PRECEDENCE.put(Operator.GT, 8);
		PRECEDENCE.put(Operator.LTE, 8);
		PRECEDENCE.put(Operator.GTE, 8);
	    // 9. Битовые сдвиги
		PRECEDENCE.put(Operator.SHL, 9);
		PRECEDENCE.put(Operator.SHR, 9);
		// 10. Сложение/вычитание
		PRECEDENCE.put(Operator.PLUS, 10);
	    PRECEDENCE.put(Operator.MINUS, 10);
	    // 11. Умножение/деление
	    PRECEDENCE.put(Operator.MULT, 11);
	    PRECEDENCE.put(Operator.DIV, 11);
	    PRECEDENCE.put(Operator.MOD, 11);
		// 12. Унарные операторы (высший приоритет)
		PRECEDENCE.put(Operator.NOT, 12);
		PRECEDENCE.put(Operator.BIT_NOT, 12);
		// Унарные + и - обрабатываются в parseUnary()
	};
	
	private final String symbol;

	Operator(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public static Operator fromSymbol(String symbol) {
		for (Operator op : values()) {
			if (op.symbol.equals(symbol)) {
				return op;
			}
		}
		return null;
	}
}
