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

package ru.vm5277.compiler;

import ru.vm5277.common.lexer.Lexer;
import ru.vm5277.common.lexer.J8BKeyword;
import ru.vm5277.common.lexer.TokenType;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.lexer.LexerType;
import ru.vm5277.common.lexer.tokens.Token;
import static ru.vm5277.common.lexer.tokens.Token.toStringValue;

public class LexerTests {

    @Test
    public void testBasicTypesAndKeywords() throws Exception {		
		String source = "true false null " + 
						"void bool byte short int fixed cstr " +
						"if do while for return continue break switch " +
						"static final private public native atomic " +
						"class interface implements this " +
						"import as else case default new " +
						"try catch throws";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(37, tokens.size() - 1); // -1 для EOF
        
		int pos=0;
		// Проверка литералов
		assertEquals(TokenType.LITERAL, tokens.get(pos).getType());
		assertEquals(true, tokens.get(pos++).getValue());
		assertEquals(TokenType.LITERAL, tokens.get(pos).getType());
		assertEquals(false, tokens.get(pos++).getValue());
		assertEquals(TokenType.LITERAL, tokens.get(pos).getType());
		assertEquals(null, tokens.get(pos++).getValue());

		// Проверка типов
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.VOID, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.BOOL, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.BYTE, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.SHORT, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.INT, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.FIXED, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(J8BKeyword.CSTR, tokens.get(pos++).getValue());

		// Проверка комманд
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.IF, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.DO, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.WHILE, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.FOR, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.RETURN, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.CONTINUE, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.BREAK, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.SWITCH, tokens.get(pos++).getValue());

		// Проверка модификаторов
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(J8BKeyword.STATIC, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(J8BKeyword.FINAL, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(J8BKeyword.PRIVATE, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(J8BKeyword.PUBLIC, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(J8BKeyword.NATIVE, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(J8BKeyword.ATOMIC, tokens.get(pos++).getValue());

		// Проверка ключевых слов ООП
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(J8BKeyword.CLASS, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(J8BKeyword.INTERFACE, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(J8BKeyword.IMPLEMENTS, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(J8BKeyword.THIS, tokens.get(pos++).getValue());
		
		// Остальное
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.IMPORT, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.AS, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.ELSE, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.CASE, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.DEFAULT, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.NEW, tokens.get(pos++).getValue());


		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(J8BKeyword.TRY, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(J8BKeyword.CATCH, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(J8BKeyword.THROWS, tokens.get(pos++).getValue());

	}

    @Test
    public void testIntegerLiterals() throws Exception {
        String source = "42 0x12fA 0b1010 05277 1 256 65536 4294967296 0x7fffffffffffffff 0";
        //String source = "0x7fffffffffffffff";
        
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(10, tokens.size() - 1);
        
        // Десятичное число
        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals(42, tokens.get(0).getValue());
        
        // Шестнадцатеричное число
        assertEquals(TokenType.NUMBER, tokens.get(1).getType());
        assertEquals(4858, tokens.get(1).getValue());
        
        // Двоичное число
        assertEquals(TokenType.NUMBER, tokens.get(2).getType());
        assertEquals(10, tokens.get(2).getValue());
        
        // Восьмеричное число
        assertEquals(TokenType.NUMBER, tokens.get(3).getType());
        assertEquals(2751, tokens.get(3).getValue());

		// Integer
        assertEquals(TokenType.NUMBER, tokens.get(4).getType());
        assertEquals(1, tokens.get(4).getValue());
        assertEquals(TokenType.NUMBER, tokens.get(5).getType());
        assertEquals(256, tokens.get(5).getValue());
        assertEquals(TokenType.NUMBER, tokens.get(6).getType());
        assertEquals(65536, tokens.get(6).getValue());
        
		//Long
		assertEquals(TokenType.NUMBER, tokens.get(7).getType());
        assertEquals(4294967296l, tokens.get(7).getValue());
        
		//Long
		assertEquals(TokenType.NUMBER, tokens.get(8).getType());
        assertEquals("9223372036854775807", tokens.get(8).getStringValue());

		//Zero
		assertEquals(TokenType.NUMBER, tokens.get(9).getType());
        assertEquals(0, tokens.get(9).getValue());

	}

    @Test
    public void testFixedPointLiterals() throws Exception {
        String source = "3.141 127.001 0.0000000001";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(3, tokens.size() - 1); // -1 для EOF
        
        assertEquals(TokenType.NUMBER, tokens.get(0).getType());
        assertEquals("3.141", tokens.get(0).getStringValue());
        assertEquals(TokenType.NUMBER, tokens.get(1).getType());
        assertEquals("127.001", tokens.get(1).getStringValue());
        assertEquals(TokenType.NUMBER, tokens.get(2).getType());
        assertEquals("0.0000000001", tokens.get(2).getStringValue());
    }

    @Test
    public void testOperators() throws Exception {
        String source = "= + - * / % == != < > <= >=";	//TODO добавить остальные?

		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(12, tokens.size() - 1);
        
        assertEquals(TokenType.OPERATOR, tokens.get(0).getType());
        assertEquals(Operator.ASSIGN, tokens.get(0).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(1).getType());
        assertEquals(Operator.PLUS, tokens.get(1).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(2).getType());
        assertEquals(Operator.MINUS, tokens.get(2).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(3).getType());
        assertEquals(Operator.MULT, tokens.get(3).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(4).getType());
        assertEquals(Operator.DIV, tokens.get(4).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(5).getType());
        assertEquals(Operator.MOD, tokens.get(5).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(6).getType());
        assertEquals(Operator.EQ, tokens.get(6).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(7).getType());
        assertEquals(Operator.NEQ, tokens.get(7).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(8).getType());
        assertEquals(Operator.LT, tokens.get(8).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(9).getType());
        assertEquals(Operator.GT, tokens.get(9).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(10).getType());
        assertEquals(Operator.LTE, tokens.get(10).getValue());
        assertEquals(TokenType.OPERATOR, tokens.get(11).getType());
        assertEquals(Operator.GTE, tokens.get(11).getValue());
    }
	
    @Test
    public void testNotes() throws Exception {
        String source = "#p/8e5-e5-e5-c5-e5-/4g5-g4";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(1, tokens.size() - 1); // -1 для EOF
        
		assertEquals(TokenType.NOTE, tokens.get(0).getType());
		assertEquals("0x884444444044844838", toStringValue(tokens.get(0).getValue()));
    }
	
    @Test
    public void testLabel() throws Exception {
        String source = "label1:";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(1, tokens.size() - 1); // -1 для EOF
        
		assertEquals(TokenType.LABEL, tokens.get(0).getType());
		assertEquals("label1", toStringValue(tokens.get(0).getValue()));
    }

	@Test
	public void testErrorMessages1() throws Exception {
		String source = "/*ndkfhskhfk*/ #g4534534;";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		List<Token> invalidTokents = lexer.getInvalidTokens();
		assertEquals(1, invalidTokents.size());

		assertEquals(true, invalidTokents.get(0).getStringValue().equals("Unsupported #block: \'g\'"));
		assertEquals(1, invalidTokents.get(0).getSP().getLine());
		assertEquals(17, invalidTokents.get(0).getSP().getColumn());
		
	}
	
}
