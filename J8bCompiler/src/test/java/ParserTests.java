import ru.vm5277.j8b.compiler.Lexer;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ru.vm5277.j8b.compiler.Parser;
import ru.vm5277.j8b.compiler.nodes.FieldNode;
import ru.vm5277.j8b.compiler.nodes.expressions.LiteralExpression;

public class ParserTests {

	@Test
	public void constantFoldingTest() throws Exception {
		String source = "class A{	int i1=5+3-4/2*10; int i2=10%3; int i3=0b11001011 & 0x0f; int i4=0b11001011 | 04; int i5= 2*(4+3); int i6=5+4*3;" +
								"	fixed f1=2.5+3.5; fixed f2=5-1.5; fixed f3=4.5/1.5; fixed f4=25*0.1;" +
								"	byte[] s1=\"a\"+\"b\"; byte[] s2=\"a\"+123; byte[] s3=\"z\"+0.9; byte[] s4=\"ZX Spectru\"+'m'; byte[] s5='1'+'z';}";
//		String source = "class A{ str s4=\"ZX Spectru\"+'m';}"
		Lexer lexer = new Lexer(new StringReader(source));
		Parser parser = new Parser(lexer.getTokens());
		
		//int
		FieldNode field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(0);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigInteger);
		assertEquals("-12", ((LiteralExpression)field.getInitializer()).getValue().toString());
		
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(1);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigInteger);
		assertEquals("1", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(2);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigInteger);
		assertEquals("11", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(3);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigInteger);
		assertEquals("207", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(4);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigInteger);
		assertEquals("14", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(5);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigInteger);
		assertEquals("17", ((LiteralExpression)field.getInitializer()).getValue().toString());



		//fixed
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(6);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigDecimal);
		assertEquals("6.0", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(7);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigDecimal);
		assertEquals("3.5", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(8);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigDecimal);
		assertEquals("3", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(9);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof BigDecimal);
		assertEquals("2.5", ((LiteralExpression)field.getInitializer()).getValue().toString());
		
		//string
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(10);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("ab", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(11);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("a123", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(12);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("z0.9", ((LiteralExpression)field.getInitializer()).getValue().toString());
		
		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(13);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("ZX Spectrum", ((LiteralExpression)field.getInitializer()).getValue().toString());

		field = (FieldNode)parser.getAst().getDeclarations().get(0).getBlocks().get(0).getDeclarations().get(14);
		assertEquals(true, field.getInitializer() instanceof LiteralExpression);
		assertEquals(true, ((LiteralExpression)field.getInitializer()).getValue() instanceof String);
		assertEquals("1z", ((LiteralExpression)field.getInitializer()).getValue().toString());
		
	}
}
