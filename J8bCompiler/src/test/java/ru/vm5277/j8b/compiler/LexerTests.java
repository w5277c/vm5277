package ru.vm5277.j8b.compiler;

import ru.vm5277.j8b.compiler.tokens.Token;
import ru.vm5277.j8b.compiler.enums.TokenType;
import ru.vm5277.j8b.compiler.enums.Keyword;
import java.io.StringReader;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.messages.ErrorMessage;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import static ru.vm5277.j8b.compiler.tokens.Token.toStringValue;

public class LexerTests {

    @Test
    public void testBasicTypesAndKeywords() throws Exception {
        assertEquals(35, Keyword.values().length);
		
		String source = "true false null " + 
						"void bool byte short int fixed cstr " +
						"if do while for return continue break switch free " +
						"static final private public native atomic " +
						"class interface implements this " +
						"import as else case default new";
        MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(35, tokens.size() - 1); // -1 для EOF
        
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
		assertEquals(Keyword.VOID, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(Keyword.BOOL, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(Keyword.BYTE, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(Keyword.SHORT, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(Keyword.INT, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(Keyword.FIXED, tokens.get(pos++).getValue());
		assertEquals(TokenType.TYPE, tokens.get(pos).getType());
		assertEquals(Keyword.CSTR, tokens.get(pos++).getValue());

		// Проверка комманд
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.IF, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.DO, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.WHILE, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.FOR, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.RETURN, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.CONTINUE, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.BREAK, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.SWITCH, tokens.get(pos++).getValue());
		assertEquals(TokenType.COMMAND, tokens.get(pos).getType());
		assertEquals(Keyword.FREE, tokens.get(pos++).getValue());

		// Проверка модификаторов
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(Keyword.STATIC, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(Keyword.FINAL, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(Keyword.PRIVATE, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(Keyword.PUBLIC, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(Keyword.NATIVE, tokens.get(pos++).getValue());
		assertEquals(TokenType.MODIFIER, tokens.get(pos).getType());
		assertEquals(Keyword.ATOMIC, tokens.get(pos++).getValue());

		// Проверка ключевых слов ООП
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(Keyword.CLASS, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(Keyword.INTERFACE, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(Keyword.IMPLEMENTS, tokens.get(pos++).getValue());
		assertEquals(TokenType.OOP, tokens.get(pos).getType());
		assertEquals(Keyword.THIS, tokens.get(pos++).getValue());
		
		// Остальное
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(Keyword.IMPORT, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(Keyword.AS, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(Keyword.ELSE, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(Keyword.CASE, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(Keyword.DEFAULT, tokens.get(pos++).getValue());
		assertEquals(TokenType.KEYWORD, tokens.get(pos).getType());
		assertEquals(Keyword.NEW, tokens.get(pos++).getValue());

	}

    @Test
    public void testIntegerLiterals() throws Exception {
        String source = "42 0x12fA 0b1010 05277 1 256 65536 4294967296 0x7fffffffffffffff 0";
        //String source = "0x7fffffffffffffff";
        
		
		MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
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
        MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
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
        MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
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
        MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(1, tokens.size() - 1); // -1 для EOF
        
		assertEquals(TokenType.NOTE, tokens.get(0).getType());
		assertEquals("0x884444444044844838", toStringValue(tokens.get(0).getValue()));
    }
	
    @Test
    public void testLabel() throws Exception {
        String source = "label1:";
        MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
        List<Token> tokens = lexer.getTokens();
        
        assertEquals(1, tokens.size() - 1); // -1 для EOF
        
		assertEquals(TokenType.LABEL, tokens.get(0).getType());
		assertEquals("label1", toStringValue(tokens.get(0).getValue()));
    }

	@Test
	public void testErrorMessages1() throws Exception {
		String source = "/*ndkfhskhfk*/ #g4534534;";
        MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader(source), mc);
		List<ErrorMessage> messages = mc.getErrorMessages();
		assertEquals(1, messages.size());
		ErrorMessage em = messages.get(0);
		assertEquals(true, em.getText().toLowerCase().startsWith("unsupported #block"));
		assertEquals(1, em.getSP().getLine());
		assertEquals(17, em.getSP().getColumn());
		
	}
	
}
