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
package ru.vm5277.compiler.nodes;

import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.compiler.Operand;
import ru.vm5277.common.compiler.OperandType;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.ParseException;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.nodes.expressions.BinaryExpression;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.VariableExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.Symbol;

public class VarNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private	final	VarType			type;
	private	final	String			name;
	private			ExpressionNode	initializer;
	private			Symbol			symbol;
	
	public VarNode(TokenBuffer tb, MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(tb, mc);
		
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;

		if (!tb.match(Operator.ASSIGN)) {
            initializer = null;
        }
		else {
			consumeToken(tb);
			try {initializer = new ExpressionNode(tb, mc).parse();} catch(ParseException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(ParseException e) {markFirstError(e);}
	}

	public VarNode(MessageContainer mc, Set<Keyword> modifiers, VarType type, String name) {
		super(null, mc);
		
		this.modifiers = modifiers;
		this.type = type;
		this.name = name;
	}
	
	public VarType getType() {
		return type;
	}
	
	public String getName() {
		return name;
	}
	
	public ExpressionNode getInitializer() {
		return initializer;
	}
	
	public boolean isFinal() {
		return modifiers.contains(Keyword.FINAL);
	}

	public Set<Keyword> getModifiers() {
		return modifiers;
	}
	
	@Override
	public String getNodeType() {
		return "var";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + modifiers + ", " + type + ", " + name;
	}

	@Override
	public boolean preAnalyze() {
		if(Character.isUpperCase(name.charAt(0))) addMessage(new WarningMessage("Variable name should start with lowercase letter:" + name, sp));

		return true;
	}

	
	@Override
	public boolean declare(Scope scope) {
		if(scope instanceof BlockScope) {
			BlockScope blockScope = (BlockScope)scope;
			boolean isFinal = modifiers.contains(Keyword.FINAL) || VarType.CSTR == type;
			symbol = new Symbol(name, type, isFinal, modifiers.contains(Keyword.STATIC));
			if(isFinal && null != initializer) {
				if(initializer instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)initializer;
					symbol.setConstantOperand(new Operand(le.getType(scope), OperandType.LITERAL, le.getValue()));
				}
				else if(initializer instanceof VariableExpression) {
					VariableExpression ve = (VariableExpression)initializer;
					symbol.setConstantOperand(new Operand(ve.getType(scope), OperandType.TYPE, ve.getValue()));
				}
			}

			try {blockScope.addLocal(symbol);}
			catch(SemanticException e) {markError(e);}
		}
		else markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in var:" + name);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope) {
		// Проверка инициализации final-полей
		if (isFinal() && initializer == null) markError("Final variable  '" + name + "' must be initialized");

		// Анализ инициализатора, если есть
		if (initializer != null) initializer.postAnalyze(scope);

		// Проверка совместимости типов
		try {
			VarType initType = initializer.getType(scope);
			if (!isCompatibleWith(scope, type, initType)) {
				markError("Type mismatch: cannot assign " + initType + " to " + type);
			}
			
			// Дополнительная проверка на сужающее преобразование
			if (type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) { //TODO верятно нужно и в других местах
				markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast"); 
			}
		}
		catch (SemanticException e) {markError(e);}
		
		return true;
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		boolean isConstant = isFinal() || VarType.CSTR == type;
		int runtimeId = cg.enterLocal(type.getId(), type.getSize(), isConstant, name);
		symbol.setRuntimeId(runtimeId);
		if(isConstant) {
			if(initializer instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)initializer;
				if(VarType.CSTR == le.getType(null)) {
					cg.defineStr(runtimeId, (String)le.getValue());
				}
				else {
//TODO вроде как не нужно					cg.defineData(runtimeId, type.getSize(), le.getNumValue());
				}
			}
			else throw new Exception("unexpected expression:" + initializer + " for constant");
		}
		else {
			if(initializer instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				cg.localStore(runtimeId, ((LiteralExpression)initializer).getNumValue());
			}
			else {
				if(initializer instanceof BinaryExpression) {
					cg.enterExpression();
					initializer.codeGen(cg);
					cg.storeAcc(runtimeId);
					cg.leaveExpression();
				}
				else {
					initializer.codeGen(cg);
					cg.loadRegs(type.getSize());
					cg.storeAcc(runtimeId);
				}
			}
		}
		cg.leaveLocal();
	}
}