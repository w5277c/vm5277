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
import ru.vm5277.common.cg.scopes.CGCellsScope;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;
import ru.vm5277.compiler.tokens.Token;

public class LiteralExpression extends ExpressionNode {
    private Object value;
    
    public LiteralExpression(TokenBuffer tb, MessageContainer mc, Object value) {
        super(tb, mc);
        
		this.value = value;
    }
    
	@Override
	public VarType getType(Scope scope) {
		if (value == null) return VarType.NULL;
		if (value instanceof Boolean) return VarType.BOOL;
		if (value instanceof Number)  {
			if(value instanceof Double) return VarType.FIXED;
			
			long l = ((Number)value).longValue();
			if(l<0) return VarType.FIXED;
			if(l<=255) return VarType.BYTE;
			if(l<=65535) return VarType.SHORT;
			return VarType.INT;
		}
		if (value instanceof Character) return VarType.BYTE;
		if (value instanceof String) return VarType.CSTR;
		if (value instanceof VarType) return (VarType)value;
		return VarType.UNKNOWN;
	}	

	public Object getValue() {
		return value;
	}
	
	public boolean isInteger() {
		return value instanceof Integer || value instanceof Long;
	}
	
	public boolean isCstr() {
		return value instanceof String;
	}
	
	public long getNumValue() {
		if(value instanceof Character) {
			return (int)(Character)value;
		}
		else if(value instanceof Boolean) {
			return ((Boolean)value) ? 0x01 : 0x00;
		}
		else if(value instanceof VarType) {
			return ((VarType)value).getId();
		}
		return ((Number)value).longValue();
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + Token.toStringValue(value);
    }
	
	@Override
	public boolean preAnalyze() {
		if (value == null) {
			markError("Literal value cannot be null");
			return false;
		}
		return true;
	}
	
	@Override
	public boolean postAnalyze(Scope scope, CodeGenerator cg) {
		try {
			cgScope = cg.enterExpression();
			if (value instanceof Number) {
				VarType type = getType(scope);
				if (type == VarType.UNKNOWN) {
					markError("Unsupported numeric literal type");
					cg.leaveExpression();
					return false;
				}
				// Проверка диапазона через VarType.checkRange
				type.checkRange((Number)value);
			}
			
			cg.leaveExpression();
			return true;
		} 
		catch (CompileException e) {
			markError(e.getMessage());

			cg.leaveExpression();
			return false;
		}
	}
	
	@Override
	public Object codeGen(CodeGenerator cg) throws Exception {
		//TODO перенести код в VarNode и FieldNode
		if(cgScope.getParent() instanceof CGCellsScope) {
			CGCellsScope cScope = (CGCellsScope)cgScope.getParent();

//			if(VarType.NULL == cScope.getType()) {
//				cg.setAcc(cScope, 0x01, 0);
//			}
			//accum.set(scope, size, value);
			cg.constToAcc(cScope, cScope.getSize(), getNumValue());
		}
		return null;
	}
}