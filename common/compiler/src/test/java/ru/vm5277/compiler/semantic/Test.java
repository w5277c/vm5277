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

package ru.vm5277.compiler.semantic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.Optimization;
import ru.vm5277.compiler.ASTParser;
import ru.vm5277.compiler.Lexer;
import ru.vm5277.compiler.SemanticAnalyzer;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.codegen.PlatformLoader;

public class Test {
    @org.junit.jupiter.api.Test
    void test1() throws CompileException {
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
    void test2() throws CompileException, IOException, CompileException, Exception {
		ClassScope globalScope = new ClassScope();
		Path toolkitPath = FSUtils.getToolkitPath();
		File libDir = toolkitPath.resolve("bin").resolve("libs").normalize().toFile();
		CodeGenerator cg = PlatformLoader.loadGenerator("stub", libDir, Optimization.SIZE, null, null);
		MessageContainer mc = new MessageContainer(100, true, false);
		Lexer lexer = new Lexer("class Clazz{ void method() { byte b1 = -1; byte b2=0; byte b3=255; byte b4 = 256; byte B5=128; }}", mc);
		ASTParser parser = new ASTParser(null, null, lexer.getTokens(), mc);
		SemanticAnalyzer.analyze(globalScope, parser.getClazz(), cg);
	}
}
