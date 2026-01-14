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
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.vm5277.common.lexer.Operator;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.FieldNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.bin.BinaryExpression;
import ru.vm5277.common.lexer.LexerType;
import static ru.vm5277.common.lexer.tokens.Token.toStringValue;

public class ParserTests {
	@Test
	public void constantFoldingTest() throws Exception {
		String source = "class A{	int i1=5+3-4/2*10; int i2=10%3; int i3=0b11001011 & 0x0f; int i4=0b11001011 | 04; int i5= 2*(4+3); int i6=5+4*3;" +
								"	fixed f1=2.5+3.5; fixed f2=5-1.5; fixed f3=4.5/1.5; fixed f4=25*0.1;" +
								"	byte[] s1=\"a\"+\"b\"; byte[] s2=\"a\"+123; byte[] s3=\"z\"+0.9; byte[] s4=\"ZX Spectru\"+'m'; byte[] s5=\"1\"+'z';" +
								"	bool b1=true && false; bool b2=true || false; bool b3=true == true; bool b4=true!=true; bool b5 = !true;" +
								"	bool b6=!(65>3); bool b7=3<=3; bool b8=5%2==1; " +
								"	int t1=-5; int t2=5+i1+i2-2-i3; int t3=7==10-6/2?7:2; int t4=5<3 ? 7:2; int t5=true?1:0;}";

		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		ASTParser parser = new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);

		FieldNode field = (FieldNode)parser.getClazz().getBody().getChildren().get(0);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("-12", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(1);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("1", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(2);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Long);
		assertEquals("11", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(3);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("207", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(4);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("14", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(5);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("17", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

		field = (FieldNode)parser.getClazz().getBody().getChildren().get(6);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("6", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(7);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("3.5", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(8);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("3", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(9);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Double);
		assertEquals("2.5", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

		field = (FieldNode)parser.getClazz().getBody().getChildren().get(10);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("ab", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(11);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("a123", ((LiteralExpression)field.getInitializer()).getValue().toString());
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(12);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("z0.9", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(13);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("ZX Spectrum", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(14);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("1z", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

		field = (FieldNode)parser.getClazz().getBody().getChildren().get(15);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(16);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(17);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(18);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(19);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", ((LiteralExpression)field.getInitializer()).getValue().toString());
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(20);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(21);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(22);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		

		field = (FieldNode)parser.getClazz().getBody().getChildren().get(23);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("-5", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(24);
		assertEquals(true, field.getInitializer() instanceof BinaryExpression);
		BinaryExpression e1 = (BinaryExpression)field.getInitializer();
		assertEquals(Operator.PLUS, e1.getOperator());
		assertEquals(true, e1.getLeft() instanceof BinaryExpression);
		BinaryExpression e2 = (BinaryExpression)e1.getLeft();
		assertEquals(Operator.MINUS, e2.getOperator());
		assertEquals(true, e2.getLeft() instanceof BinaryExpression);
		BinaryExpression e3 = (BinaryExpression)e2.getLeft();
		assertEquals(Operator.PLUS, e3.getOperator());
		assertEquals(true, e1.getRight() instanceof LiteralExpression);
		assertEquals("3", ((LiteralExpression)e1.getRight()).getValue().toString());
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(25);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("7", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(26);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("2", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getClazz().getBody().getChildren().get(27);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("1", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
	}

	@Test
	public void incrementDecrementTest() throws Exception {
		String source = "class A{ public A() {int i=1; i++; i--; ++i; --i;}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}
	
	@Test
	public void ifTest() throws Exception {
		String source = "class A{ public A() { int r=0; if(5>3) r=1; if(6>3) {r=1;} else r=2; if(5>3) {r=1;r++;} else {r=2;r++;}}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}
	
	@Test
	public void doWhileTest() throws Exception {
		String source = "class A{ public A() { int i=0; do { i++; } while(i<10);}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}

	@Test
	public void whileTest() throws Exception {
		String source = "class A{ public A() { int i=0; while(i<10) { i++; }}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}

	@Test
	public void forTest() throws Exception {
		String source = "class A{ public A() { int i=0; for(int f=0; f<10; f++) { if(1==f) continue; if(2==f) break; } else {i+=10;}}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}

	@Test
	public void returnTest() throws Exception {
		String source = "class A{ public int A() { return 123+i;} void B() { return;}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}

	@Test
	public void switchTest() throws Exception { //TODO
		String source ="class A{ public int A() { int t=0; switch(t) { case 0: return 1; case 1..10: return 2; case 11: {t++; return 3;} default: return 4;}}}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}

	@Test
	public void someTest1() throws Exception {
		String source ="class A{ public int A() { if(count %2 == 0) {} }}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}
	
	@Test
	public void someTest2() throws Exception {
		String source ="class A{ public int A() { return -a-b; }}";
		Lexer lexer = new Lexer(LexerType.J8B, source, false, 0x04);
		new ASTParser(null, null, lexer.getTokens(), new MessageContainer(100, true, false), 0x04);
		assertEquals(0, lexer.getInvalidTokens().size());
	}
	
}
