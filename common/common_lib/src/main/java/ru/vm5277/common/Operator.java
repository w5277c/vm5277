/*
 * Copyright 2025 konstantin@5277.ru
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.vm5277.common;

import java.util.HashMap;
import java.util.Map;

public enum Operator {
	// Базовые арифметические
	PLUS("+"), MINUS("-"), MULT("*"), DIV("/"), MOD("%"),
    // Инкремент/декремент
    INC("++"), DEC("--"), PRE_INC("++"), PRE_DEC("--"),	POST_INC("++"), POST_DEC("--"), //Лексер выберет первые два, а парсер PRE или POST
	// Сравнения
	EQ("=="), NEQ("!="), LT("<"), GT(">"), LTE("<="), GTE(">="),
	// Логические
	AND("&&"), OR("||"), NOT("!"),
	// Битовые
	BIT_AND("&"), BIT_OR("|"), BIT_XOR("^"), BIT_NOT("~"), SHL("<<"), SHR(">>"),
	// Присваивание (простое и составное)
	ASSIGN("="), PLUS_ASSIGN("+="), MINUS_ASSIGN("-="), MULT_ASSIGN("*="), DIV_ASSIGN("/="), MOD_ASSIGN("%="), AND_ASSIGN("&="), OR_ASSIGN("|="),
	XOR_ASSIGN("^="), SHL_ASSIGN("<<="), SHR_ASSIGN(">>="),
	//Тернарный и instanceof
	TERNARY("?"), IS("is");

	
	public static final Map<Operator, Integer> PRECEDENCE = new HashMap<>();
	static {
	    // 0. Тенарный оператор
		PRECEDENCE.put(Operator.TERNARY, 0);
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
	    PRECEDENCE.put(Operator.IS, 7);
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
		PRECEDENCE.put(Operator.PRE_INC, 12);
		PRECEDENCE.put(Operator.PRE_DEC, 12);
        // 13. Постфиксные операторы (наивысший приоритет)
        PRECEDENCE.put(Operator.INC, 13);
        PRECEDENCE.put(Operator.DEC, 13);
		PRECEDENCE.put(Operator.POST_INC, 13);
		PRECEDENCE.put(Operator.POST_DEC, 13);

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
			if (op.symbol.equals(symbol) && Operator.PRE_DEC != op && Operator.PRE_INC != op && Operator.POST_DEC != op && Operator.POST_INC != op) {
				return op;
			}
		}
		return null;
	}
	
	// Проверяет, применимо ли для ассемблера (В ассемблере оператором сравнения также является '=')
	public boolean isAsmCompatible() {
		return	this == PLUS || this == MINUS || this == DIV || this == MOD || this == MULT ||
				this == ASSIGN || this == EQ || this == NEQ || this == LT || this == LTE || this == GT || this == GTE ||
				this == AND || this == OR || this == NOT ||
				this == BIT_AND || this == BIT_OR || this == BIT_NOT || this == BIT_XOR || this == SHL || this == SHR;
	}

	// Проверяет, является ли оператор сравнением
	public boolean isComparison() {
		return this == EQ || this == NEQ || this == LT || this == GT || this == LTE || this == GTE || this == IS;
	}

	// Проверяет, является ли оператор логическим
	public boolean isLogical() {
		return this == AND || this == OR || this == NOT;
	}

	// Проверяет, является ли оператор битовым
	public boolean isBitwise() {
		return this == BIT_AND || this == BIT_OR || this == BIT_XOR || this == BIT_NOT || this == SHL || this == SHR;
	}

	// Проверяет, является ли оператор арифметическим
	public boolean isArithmetic() {
		return	this == PLUS || this == MINUS || this == MULT || this == DIV || this == MOD || this == INC || this == DEC ||
				this == PRE_INC || this == PRE_DEC || this == POST_INC || this == POST_DEC;
	}

	// Проверяет, является ли оператор присваиванием
	public boolean isAssignment() {
		return	this == ASSIGN || this == PLUS_ASSIGN || this == MINUS_ASSIGN || this == MULT_ASSIGN || this == DIV_ASSIGN || this == MOD_ASSIGN || 
				this == AND_ASSIGN || this == OR_ASSIGN || this == XOR_ASSIGN || this == SHL_ASSIGN || this == SHR_ASSIGN;
	}

	// Проверяет, является ли оператор унарным
	public boolean isUnary() {
		return this == NOT || this == BIT_NOT || this == PLUS || this == MINUS || this == PRE_INC || this == PRE_DEC;
	}

	// Проверяет, является ли оператор постфиксным
    public boolean isPostfix() {
        return this == POST_INC || this == POST_DEC;
    }

	// Проверяет, можно ли применять оператор к boolean
	public boolean isBooleanOperator() {
		return isLogical() || (isComparison() && !isBitwise());
	}

	// Проверяет, можно ли применять оператор к числам
	public boolean isNumericOperator() {
		return	isArithmetic() || isBitwise() || this == PLUS || this == MINUS || this == INC || this == DEC || this == PRE_INC || this == PRE_DEC ||
				this == POST_INC || this == POST_DEC;
	}
	
	public boolean isCommutative() {
		return this == PLUS || this == MULT || this == BIT_AND || this == BIT_OR || this == BIT_XOR || this == EQ || this == NEQ;
	}

	// Проверяет, является ли оператор левоассоциативным
	public boolean isLeftAssociative() {
		return !(this.isAssignment() || this == Operator.TERNARY);
	}
}
