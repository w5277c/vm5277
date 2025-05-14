/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.j8b.compiler.semantic;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import ru.vm5277.j8b.compiler.ASTParser;
import ru.vm5277.j8b.compiler.Lexer;
import ru.vm5277.j8b.compiler.SemanticAnalyzer;
import ru.vm5277.j8b.compiler.enums.Keyword;
import ru.vm5277.j8b.compiler.enums.Operator;
import ru.vm5277.j8b.compiler.enums.VarType;
import ru.vm5277.j8b.compiler.exceptions.ParseException;
import ru.vm5277.j8b.compiler.exceptions.SemanticException;
import ru.vm5277.j8b.compiler.messages.MessageContainer;
import ru.vm5277.j8b.compiler.nodes.BlockNode;
import ru.vm5277.j8b.compiler.nodes.ClassBlockNode;
import ru.vm5277.j8b.compiler.nodes.ClassNode;
import ru.vm5277.j8b.compiler.nodes.FieldNode;
import ru.vm5277.j8b.compiler.nodes.MethodNode;
import ru.vm5277.j8b.compiler.nodes.ParameterNode;
import ru.vm5277.j8b.compiler.nodes.VarNode;
import ru.vm5277.j8b.compiler.nodes.commands.ReturnNode;
import ru.vm5277.j8b.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.j8b.compiler.nodes.expressions.TernaryExpression;
import ru.vm5277.j8b.compiler.nodes.expressions.VariableExpression;

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
