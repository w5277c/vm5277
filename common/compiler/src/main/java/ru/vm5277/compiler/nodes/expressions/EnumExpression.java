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

package ru.vm5277.compiler.nodes.expressions;

import ru.vm5277.common.cg.CodeGenerator;
import ru.vm5277.common.cg.scopes.CGScope;
import ru.vm5277.common.compiler.CodegenResult;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.Delimiter;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

public class EnumExpression extends ExpressionNode {
	private	String	id;
	private	String	value;
	
	private	int		index;
	private	VarType type;

	public EnumExpression(TokenBuffer tb, MessageContainer mc, String id) throws CompileException {
		super(tb, mc);
		
		this.id = id;
		consumeToken(tb, Delimiter.DOT);
		value = tb.consume().getValue().toString();
	}
	
	@Override
	public VarType getType(Scope scope) {
		return type;
	}
	
	public int getIndex() {
		return index;
	}
	
	@Override
	public boolean preAnalyze() {
		boolean result = true;
		
		type = VarType.fromEnumName(id);
		index = type.getEnumValueIndex(value);

		if(0>index) {
			markError("Invalid enum value '" + value + "' for enum " + id);
			result = false;
		}
		if(255<index) {
			markError("Enum value index out of range (0-255): " + index);
			result = false;
		}

		return result;
	}
	
	@Override
	public boolean declare(Scope scope) {
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		return true;
	}

	public String getName() {
		return id + "." + value;
	}
	
	@Override
	public Object codeGen(CodeGenerator cg, CGScope parent, boolean toAccum) throws Exception {
		if(toAccum) {
			cg.constToAcc(parent, 0x01, index, false);
			return CodegenResult.RESULT_IN_ACCUM;
		}
		
		return null;
	}
}
