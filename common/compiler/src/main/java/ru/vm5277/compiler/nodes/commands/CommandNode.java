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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.vm5277.common.AssemblerInterface;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.lexer.Delimiter;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Main;
import ru.vm5277.compiler.nodes.AstNode;
import ru.vm5277.compiler.nodes.BlockNode;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.BlockScope;
import ru.vm5277.compiler.semantic.LabelSymbol;
import ru.vm5277.compiler.semantic.MethodScope;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.common.lexer.tokens.TNumber;
import ru.vm5277.common.lexer.tokens.Token;

public abstract class CommandNode extends AstNode {
	public static class AstCase extends AstNode {
		private	final	List<Integer>	values;
		private	final	BlockNode		blockNode;
		private			BlockScope		scope;
		
		public AstCase(Set<Integer> values, BlockNode block) {
			this.values = new ArrayList<>(values);
			Collections.sort(this.values);
			
			this.blockNode = block;
		}
		
		public List<Integer> getValues() {
			return values;
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

		public String getValuesAsStr() {
			StringBuilder result = new StringBuilder();
			
			int i = 0;
			while(i<values.size()) {
				int from = values.get(i);
				int to = from;

				// Ищем непрерывную последовательность
				while(i+1<values.size() && values.get(i+1)==values.get(i)+1) {
					i++;
					to = values.get(i);
				}

				if(from==to) {
					// Одиночное значение
					result.append(from).append(",");
				} else {
					// Диапазон значений
					result.append(from).append("..").append(to).append(",");
				}
				i++;
			}
			if(0!=result.length()) {
				result.deleteCharAt(result.length()-1);
			}
			return result.toString();
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
	
	protected boolean isLoopNode(CommandNode node) {
		return null != node && (node instanceof ForNode || node instanceof WhileNode || node instanceof DoWhileNode);
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
	
	protected AstCase parseCase(TokenBuffer tb, MessageContainer mc) throws CompileException {
		consumeToken(tb); // Потребляем "case"

		// Парсим значение или диапазон
		Set<Integer> values = new HashSet<>();
		while(true) {
			int num = (int)parseNumber(tb);
			if(tb.match(Delimiter.RANGE)) {
				consumeToken(tb);
				
				int to = (int)parseNumber(tb);
				if(to<num) {
					throw new CompileException("Invalid case range: " + num + ".." + to);
				}
				for(int i=num; i<=to; i++) {
					if(!values.add(i)) {
						if(AssemblerInterface.STRICT_STRONG==Main.getStrictLevel()) {
							throw new CompileException("Duplicate case value: " + i);
						}
						else if(AssemblerInterface.STRICT_LIGHT==Main.getStrictLevel()) {
							markWarning("Duplicate case value: " + i);
						}
					}
				}
			}
			else {
				if(!values.add(num)) {
					if(AssemblerInterface.STRICT_STRONG==Main.getStrictLevel()) {
						throw new CompileException("Duplicate case value: " + num);
					}
					else if(AssemblerInterface.STRICT_LIGHT==Main.getStrictLevel()) {
						markWarning("Duplicate case value: " + num);
					}
				}
			}
			
			if(!tb.match(Delimiter.COMMA)) {
				break;
			}
			
			consumeToken(tb);
		}
		consumeToken(tb, Delimiter.COLON);
		
		BlockNode blockNode = null;
		blockNode = tb.match(Delimiter.LEFT_BRACE) ? new BlockNode(tb, mc) : new BlockNode(tb, mc, parseStatement());
		
		return new AstCase(values, blockNode);
	}

	private long parseNumber(TokenBuffer tb) throws CompileException {
		Token token = consumeToken(tb);
		if(token instanceof TNumber) {
			Number number = (Number)token.getValue();
			if(number instanceof Integer || number instanceof Long) {
				return number.longValue();
			}
		}
		throw new CompileException("Expected constant value with integral type for 'case' statement");
	}
}
