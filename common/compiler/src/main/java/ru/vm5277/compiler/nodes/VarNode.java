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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.Keyword;
import ru.vm5277.common.Operator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.cg.scopes.CGVarScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.common.messages.WarningMessage;
import ru.vm5277.compiler.nodes.expressions.ExpressionNode;
import ru.vm5277.compiler.nodes.expressions.FieldAccessExpression;
import ru.vm5277.compiler.nodes.expressions.LiteralExpression;
import ru.vm5277.compiler.nodes.expressions.MethodCallExpression;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.semantic.VarSymbol;

public class VarNode extends AstNode {
	private	final	Set<Keyword>	modifiers;
	private	final	VarType			type;
	private	final	String			name;
	private			ExpressionNode	initializer;
	
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
			try {initializer = new ExpressionNode(tb, mc).parse();} catch(CompileException e) {markFirstError(e);}
		}
        try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
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

		if(null != initializer) {
			if(!initializer.preAnalyze()) {
				return false;
			}
		}
		
		return true;
	}

	
	@Override
	public boolean declare(Scope scope) {
		if(null != initializer) {
			if(!initializer.declare(scope)) {
				return false;
			}
		}
		
		if(scope instanceof BlockScope) {
			BlockScope blockScope = (BlockScope)scope;
			symbol = new VarSymbol(name, type, modifiers.contains(Keyword.FINAL) || VarType.CSTR == type, modifiers.contains(Keyword.STATIC), scope, this);
			//TODO рудимент
			/*
			if(VarType.CSTR == type && null != initializer) {
				if(initializer instanceof LiteralExpression) {
					LiteralExpression le = (LiteralExpression)initializer;
					symbol.setOperand(new Operand(le.getType(scope), OperandType.FLASH_RES, null)); //TODO лишнее
				}
				else {
					markError("Unsupported " + initializer.toString());
				}*/
/*				else if(initializer instanceof VarFieldExpression) {
					VarFieldExpression ve = (VarFieldExpression)initializer;
					symbol.setOperand(new Operand(ve.getType(scope), OperandType.TYPE, ve.getValue()));
				}*/
			/*}*/

			try {blockScope.addVariable(symbol);}
			catch(CompileException e) {markError(e);}
		}
		else markError("Unexpected scope:" + scope.getClass().getSimpleName() + " in var:" + name);

		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			symbol.setCGScope(cg.enterLocal(type, (-1 == type.getSize() ? cg.getRefSize() : type.getSize()), VarType.CSTR == type, name));
// Проверка инициализации final-полей
			if (isFinal() && initializer == null) markError("Final variable  '" + name + "' must be initialized");

			// Анализ инициализатора, если есть
			if (initializer != null) {
				if(!initializer.postAnalyze(scope, cg)) {
					cg.leaveLocal();
					return false;
				}
			}

			// Проверка совместимости типов
			if(null != initializer) {
				VarType initType = initializer.getType(scope);
				if (!isCompatibleWith(scope, type, initType)) {
					markError("Type mismatch: cannot assign " + initType + " to " + type);
				}

				// Дополнительная проверка на сужающее преобразование
				if (type.isNumeric() && initType.isNumeric() && type.getSize() < initType.getSize()) { //TODO верятно нужно и в других местах
					markError("Narrowing conversion from " + initType + " to " + type + " requires explicit cast"); 
				}

				ExpressionNode optimizedExpr = initializer.optimizeWithScope(scope);
				if(null != optimizedExpr) {
					initializer = optimizedExpr;
				}
			}
		}
		catch (CompileException e) {markError(e);}
		
		cg.leaveLocal();
		return true;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		if(cgDone) return null;
		cgDone = true;

		CGVarScope vScope = ((CGVarScope)symbol.getCGScope());
		
		// !!! Нужно,чтобы выражение строилось именно в данном CGScope(актуально, если метод вызван через depCodeGen(у него как правило другой CGScope))
		CGScope oldCGScope = cg.setScope(vScope);
		
		Boolean accUsed = null;
		vScope.build();
		if(VarType.CSTR == type) {
			if(initializer instanceof LiteralExpression) {
				LiteralExpression le = (LiteralExpression)initializer;
				if(VarType.CSTR == le.getType(null)) {
					cg.defineStr(vScope, (String)le.getValue());
					//TODO рудимент?
					//symbol.getConstantOperand().setValue(cgScope.getResId());
				}
			}
			else throw new Exception("unexpected expression:" + initializer + " for constant");
		}
		else {
			if(null == initializer) {
				cg.constToCells(cg.getScope(), vScope.getStackOffset(), 0, vScope.getCells());
			}
			else if(initializer instanceof LiteralExpression) { // Не нужно вычислять, можно сразу сохранять не используя аккумулятор
				cg.constToCells(cg.getScope(), vScope.getStackOffset(), ((LiteralExpression)initializer).getNumValue(), vScope.getCells());
			}
			else if(initializer instanceof FieldAccessExpression) {
				cg.constToCells(cg.getScope(), vScope.getStackOffset(), -1, vScope.getCells());
				accUsed = true;
			}
			else if(initializer instanceof MethodCallExpression) {
				initializer.codeGen(cg);
				cg.retToCells(cg.getScope(), vScope);
			}
			else {
				initializer.codeGen(cg);
				cg.accToCells(cg.getScope(), vScope);
				accUsed = true;
			}
		}
		cg.setScope(oldCGScope);
		return accUsed;
	}

	@Override
	public List<AstNode> getChildren() {
		if(null != initializer) return Arrays.asList(initializer);
		return null;
	}
}