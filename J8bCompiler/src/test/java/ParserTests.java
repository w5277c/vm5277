import ru.vm5277.j8b.compiler.Lexer;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.vm5277.j8b.compiler.Parser;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.nodes.FieldNode;
import ru.vm5277.j8b.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.VariableExpression;
import static ru.vm5277.j8b.compiler.tokens.Token.toStringValue;

public class ParserTests {

	@Test
	public void constantFoldingTest() throws Exception {
		String source = "class A{	int i1=5+3-4/2*10; int i2=10%3; int i3=0b11001011 & 0x0f; int i4=0b11001011 | 04; int i5= 2*(4+3); int i6=5+4*3;" +
								"	fixed f1=2.5+3.5; fixed f2=5-1.5; fixed f3=4.5/1.5; fixed f4=25*0.1;" +
								"	byte[] s1=\"a\"+\"b\"; byte[] s2=\"a\"+123; byte[] s3=\"z\"+0.9; byte[] s4=\"ZX Spectru\"+'m'; byte[] s5='1'+'z';" +
								"	bool b1=true && false; bool b2=true || false; bool b3=true == true; bool b4=true!=true; bool b5 = !true;" +
								"	bool b6=!(65>3); bool b7=3<=3;" +
								"	int t1=-5; int t2=5+i1+i2-2-i3; int t3=7==10-6/2?7:2; int t4=5<3 ? 7:2; int t5=true?1:0;}";
//		String source = "class A{ int t3=7==10-6/2?7:2;}";
		Lexer lexer = new Lexer(new StringReader(source));
		Parser parser = new Parser(lexer.getTokens());
		
		FieldNode field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(0);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("-12", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(1);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("1", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(2);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Long);
		assertEquals("11", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(3);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("207", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(4);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("14", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(5);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("17", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(6);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("6", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(7);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("3.5", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(8);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("3", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(9);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Double);
		assertEquals("2.5", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(10);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("ab", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(11);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("a123", ((LiteralExpression)field.getInitializer()).getValue().toString());
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(12);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("z0.9", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(13);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("ZX Spectrum", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(14);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("1z", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(15);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(16);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(17);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(18);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(19);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", ((LiteralExpression)field.getInitializer()).getValue().toString());
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(20);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("false", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(21);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Boolean);
		assertEquals("true", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(22);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("-5", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(23);
		assertEquals(true, field.getInitializer() instanceof BinaryExpression);
		BinaryExpression e1 = (BinaryExpression)field.getInitializer();
		assertEquals(true, e1.getLeft() instanceof BinaryExpression);
		BinaryExpression e2 = (BinaryExpression)e1.getLeft();
		assertEquals(true, e2.getLeft() instanceof BinaryExpression);
		BinaryExpression e3 = (BinaryExpression)e2.getLeft();
		assertEquals(true, e3.getLeft() instanceof LiteralExpression);
		assertEquals("3.0", ((LiteralExpression)e3.getLeft()).getValue().toString());
		assertEquals(Operator.PLUS, e3.getOperator());
		assertEquals(true, e3.getRight()instanceof VariableExpression);
		assertEquals("i1", toStringValue(((VariableExpression)e3.getRight()).getValue()));
		assertEquals(Operator.PLUS, e2.getOperator());
		assertEquals(true, e2.getRight()instanceof VariableExpression);
		assertEquals("i2", toStringValue(((VariableExpression)e2.getRight()).getValue()));
		assertEquals(Operator.MINUS, e1.getOperator());
		assertEquals(true, e1.getRight()instanceof VariableExpression);
		assertEquals("i3", toStringValue(((VariableExpression)e1.getRight()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(24);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("7", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(25);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("2", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(26);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof Number);
		assertEquals("1", toStringValue(((LiteralExpression)field.getInitializer()).getValue()));

	}
}
