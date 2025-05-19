package ru.vm5277.j8b.compiler;

import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

public class SemanticAnalyzerTests {

    @Test
    public void testVariableDeclarations() throws Exception {
//TODO        String source = "class A{int x = 42; fixed y = 3.14; /* 3.14 in Q7.8 */ }";
//        Lexer lexer = new Lexer(new StringReader(source));
//        Parser parser = new Parser(lexer.getTokens());
        
//        SemanticAnalyzer analyzer = new SemanticAnalyzer(parser.getAst());
        // Если не было исключений - тест пройден
    }

    @Test
    public void testTypeCompatibility() throws Exception {
/*        String source = "class A{int x = 42; short y = x;}"; // Должно вызвать ошибку
        Lexer lexer = new Lexer(new StringReader(source));
        ASTParser parser = new ASTParser(lexer.getTokens());
        
        try {
            SemanticAnalyzer analyzer = new SemanticAnalyzer(parser.getClazz());
            fail("Expected SemanticError for incompatible types");
        } catch (SemanticError e) {
            assertTrue(e.getMessage().contains("Type mismatch"));
        }*/
    }

    @Test
    public void testFixedPointOperations() throws Exception {
//TODO        String source = "class A{fixed a = 3.14; fixed b = 6.28; fixed c = a + b};"; // 3.14 + 6.28
//        Lexer lexer = new Lexer(new StringReader(source));
//        Parser parser = new Parser(lexer.getTokens());
        
//        SemanticAnalyzer analyzer = new SemanticAnalyzer(parser.getAst());
        // Если не было исключений - тест пройден
    }

    @Test
    public void testBooleanOperations() throws Exception {
//TODO        String source = "class A{bool a = true; bool b = false; bool c = a && b;}";
//        Lexer lexer = new Lexer(new StringReader(source));
//        Parser parser = new Parser(lexer.getTokens());
        
//        SemanticAnalyzer analyzer = new SemanticAnalyzer(parser.getAst());
        // Если не было исключений - тест пройден
    }
}