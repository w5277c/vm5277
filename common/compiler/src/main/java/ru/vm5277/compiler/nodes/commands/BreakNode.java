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
import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.TokenType;
import ru.vm5277.common.messages.MessageContainer;
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
			try {label = consumeToken(tb, TokenType.ID).toString();}catch(CompileException e) {markFirstError(e);};
		}
		
		try {consumeToken(tb, Delimiter.SEMICOLON);}catch(CompileException e) {markFirstError(e);}
        
		AstNode node = tb.getLoopStack().peek();
		if (null == node || !(node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode)) {
			markFirstError(parserError("'break' can only be used inside loop statements"));
        }
    }

	public String getLabel() {
		return label;
	}

	@Override
	public String getNodeType() {
		return "break command";
	}
	
	@Override
    public String toString() {
        return "break" + (null != label ? " " + label : "");
    }

	@Override
	public boolean preAnalyze() {
		// Проверка наличия метки (если указана)
		if (label == null) markError("Break label cannot be empty");

		// Базовая проверка - команда break должна быть внутри цикла
        if (tb.getLoopStack().isEmpty()) markError("'break' outside of loop");

		return true;
	}

	@Override
	public boolean declare(Scope scope) {
		if (label != null) {
			if (!(scope instanceof BlockScope)) markError("Labeled break must be inside block scope");
			else {
				// Разрешаем метку
				symbol = ((BlockScope)scope).resolveLabel(label);
				if (null == symbol) markError("Undefined label: " + label);

				// Регистрируем использование метки
				symbol.addReference(this);
			}
		}
		return true;
	}

	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		// Проверка для break с меткой
        if (null != symbol) {

			// Метка должна быть на цикле или switch
            if (!isLoopOrSwitch((CommandNode)tb.getLoopStack().peek())) markError("Break label must be on loop or switch statement");
            
            // Проверка видимости
            if (!isLabelInCurrentMethod(symbol, scope)) markError("Cannot break to label in different method");
        }
		return true;
	}

	@Override
	public List<AstNode> getChildren() {
		return null;
	}
}