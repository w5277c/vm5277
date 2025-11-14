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
package ru.vm5277.compiler.nodes.commands;

import java.util.List;
import static ru.vm5277.common.SemanticAnalyzePhase.DECLARE;
import static ru.vm5277.common.SemanticAnalyzePhase.POST;
import static ru.vm5277.common.SemanticAnalyzePhase.PRE;
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGLoopBlockScope;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.messages.MessageContainer;
import static ru.vm5277.compiler.Main.debugAST;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.LabelSymbol;
import ru.vm5277.compiler.semantic.Scope;

public class BreakNode extends CommandNode {
    private	String		label;
	private LabelSymbol symbol;
	
	public BreakNode(TokenBuffer tb, MessageContainer mc) {
        super(tb, mc);
        
        consumeToken(tb);
        
		if(tb.match(TokenType.ID)) {
			try {
				label = consumeToken(tb, TokenType.ID).toString();
			}
			catch(CompileException e) {
				markFirstError(e);
			}
		}
		
		try {
			consumeToken(tb, Delimiter.SEMICOLON);
		}
		catch(CompileException e) {
			markFirstError(e);
		}
    }

	public String getLabel() {
		return label;
	}

	@Override
    public String toString() {
        return (null!=label ? label : "");
    }
	public String getFullInfo() {
		return getClass().getSimpleName() + " " + toString();
	}

	@Override
	public boolean preAnalyze() {
		boolean result = true;
		debugAST(this, PRE, true, getFullInfo());

		// Базовая проверка - команда break должна быть внутри цикла
		if(!isInLoopNode()) {
			markError("'break' outside of loop");
			result = false;
        }

		debugAST(this, PRE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean declare(Scope scope) {
		boolean result = true;
		debugAST(this, DECLARE, true, getFullInfo());
		
		if(label!=null) {
			if(!(scope instanceof BlockScope)) {
				markError("Labeled break must be inside block scope");
				result = false;
			}
			else {
				// Разрешаем метку
				symbol = ((BlockScope)scope).resolveLabel(label);
				if(null==symbol) {
					markError("Undefined label: " + label);
					result = false;
				}

				if(result) {
					// Регистрируем использование метки
					symbol.addReference(this);
				}
			}
		}
		
		debugAST(this, DECLARE, false, result, getFullInfo());
		return result;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		boolean result = true;
		debugAST(this, POST, true, getFullInfo());
		cgScope = cg.enterCommand();
		
		// Проверка для break с меткой
        if(null!=symbol) {
			// Метка должна быть на цикле или switch
            if(!isLoopOrSwitch((CommandNode)tb.getLoopStack().peek())) {
				markError("Break label must be on loop or switch statement");
				result = false;
			}
            
            // Проверка видимости
            if(!isLabelInCurrentMethod(symbol, scope)) {
				markError("Cannot break to label in different method");
				result = false;
			}
        }
		
		cg.leaveCommand();
		debugAST(this, POST, false, result, getFullInfo());
		return result;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws CompileException {
		CodegenResult result = null;
		
		CGScope cgs = null == parent ? cgScope : parent;
		
		cg.jump(cgs, ((CGLoopBlockScope)cgs.getScope(CGLoopBlockScope.class)).getEndLbScope());
		
		return result;
	}
}