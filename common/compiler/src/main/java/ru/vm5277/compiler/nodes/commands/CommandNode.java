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

import java.util.Arrays;
import java.util.List;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.LabelSymbol;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.tokens.TNumber;
import ru.vm5277.compiler.tokens.Token;

public abstract class CommandNode extends AstNode {
	public static class AstCase extends AstNode {
		private	final	long		from;
		private	final	Long		to;
		private	final	BlockNode	blockNode;
		private			BlockScope	scope;
		
		public AstCase(long from, Long to, BlockNode block) {
			this.from = from;
			this.to = to;
			this.blockNode = block;
		}
		
		public long getFrom() {
			return from;
		}
		
		public Long getTo() {
			return to;
		}
		
		public BlockNode getBlock() {
			return blockNode;
		}
		
		public BlockScope getScope() {
			return scope;
		}
		public void setScope(BlockScope scope) {
			this.scope = scope;
		}

		@Override
		public List<AstNode> getChildren() {
			return Arrays.asList(blockNode);
		}
	}

	protected	CGScope	cgScope;
	
	public CommandNode(TokenBuffer tb, MessageContainer mc) {
		super(tb, mc);
	}
	
	protected boolean isLoopOrSwitch(CommandNode node) {
		return null != node && (node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode || node instanceof SwitchNode);
	}
	
	
	protected boolean isLabelInCurrentMethod(LabelSymbol symbol, Scope scope) {
		// Ищем метод в текущей цепочке областей видимости
		Scope current = scope;
		while (null != current) {
			if (current instanceof MethodScope) break;
			current = current.getParent();
		}

		// Ищем метод, содержащий метку
		Scope labelScope = symbol.getScope();
		while (null != labelScope) {
			if (labelScope instanceof MethodScope) {
				return labelScope == current;
			}
			labelScope = labelScope.getParent();
		}

		return false;
	}
	
	protected AstCase parseCase(TokenBuffer tb, MessageContainer mc) {
		consumeToken(tb); // Потребляем "case"

		// Парсим значение или диапазон
		long from = 0;
		Long to = null;

		try {from = parseNumber(tb);} catch(CompileException e) {markFirstError(e);}
		if (tb.match(Delimiter.RANGE)) {
			consumeToken(tb); // Потребляем ".."
			try{to = parseNumber(tb);}catch(CompileException e) {to=0l;markFirstError(e);}
		}

		try {consumeToken(tb, Delimiter.COLON);} catch(CompileException e) {markFirstError(e);}
		BlockNode blockNode = null;
		tb.getLoopStack().add(this);
		try {blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());}
		catch(CompileException e) {markFirstError(e);}
		tb.getLoopStack().remove(this);
		return new AstCase(from, to, blockNode);
	}

	private long parseNumber(TokenBuffer tb) throws CompileException {
		Token token = consumeToken(tb);
		if(token instanceof TNumber) {
			Number number = (Number)token.getValue();
			if(number instanceof Integer || number instanceof Long) {
				return number.longValue();
			}
		}
		throw parserError("Expected numeric value(or range) for 'case' in switch statement");
	}
}
