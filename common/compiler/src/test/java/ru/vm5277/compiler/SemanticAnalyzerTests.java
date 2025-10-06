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