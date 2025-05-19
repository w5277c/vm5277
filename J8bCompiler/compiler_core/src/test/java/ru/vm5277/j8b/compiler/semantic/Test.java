/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.j8b.compiler.semantic;

import java.io.IOException;
import java.io.StringReader;
import ru.vm5277.j8b.compiler_core.ASTParser;
import ru.vm5277.j8b.compiler_core.Lexer;
import ru.vm5277.j8b.compiler_core.SemanticAnalyzer;
import ru.vm5277.j8b.compiler.common.exceptions.ParseException;
import ru.vm5277.j8b.compiler.common.exceptions.SemanticException;
import ru.vm5277.j8b.compiler_core.messages.MessageContainer;

public class Test {
    @org.junit.jupiter.api.Test
    void test1() throws ParseException {
/*		MessageContainer mc = new MessageContainer(100, true, false);
		ClassNode classNode = new ClassNode(mc, new HashSet<Keyword>(), "Main", new ArrayList<String>());
		ClassBlockNode classBlockNode = new ClassBlockNode(mc);
		classNode.setBody(classBlockNode);

		FieldNode fieldNode1 = new FieldNode(mc, new HashSet<Keyword>(), VarType.BYTE, "f1");
		classBlockNode.getDeclarations().add(fieldNode1);

		VarNode varNode1 = new VarNode(mc, new HashSet<Keyword>(), VarType.BYTE, "v1");
		classBlockNode.getDeclarations().add(varNode1);

		List<ParameterNode> parameters = new ArrayList<>();
		parameters.add(new ParameterNode(mc, false, VarType.BYTE, "param1"));
		parameters.add(new ParameterNode(mc, false, VarType.BYTE, "param2"));
		VariableExpression varExpr1 = new VariableExpression(null, mc, "param1");
		VariableExpression varExpr2 = new VariableExpression(null, mc, "param2");
		BlockNode blockNode = new BlockNode(mc);
		ReturnNode returnNode = new ReturnNode(mc, new BinaryExpression(null, mc, varExpr1, Operator.PLUS, varExpr2));
		blockNode.getDeclarations().add(returnNode);
		MethodNode methodNode1 = new MethodNode(mc, new HashSet<Keyword>(), VarType.BYTE, "m1", parameters, blockNode);
		classBlockNode.getDeclarations().add(methodNode1);
		
		new SemanticAnalyzer(classNode);*/
    }


    @org.junit.jupiter.api.Test
    void test2() throws ParseException, IOException, SemanticException {
		MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer(new StringReader("class Clazz{ void method() { byte b1 = -1; byte b2=0; byte b3=255; byte b4 = 256; byte B5=128; }}"), mc);
		ASTParser parser = new ASTParser("", lexer.getTokens(), mc);
		new SemanticAnalyzer(null, parser.getClazz());
	}
}
