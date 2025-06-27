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
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;
import ru.vm5277.common.messages.MessageContainer;
import ru.vm5277.compiler.nodes.TokenBuffer;
import ru.vm5277.compiler.semantic.Scope;

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
		return ((Number)value).longValue();
	}
	
	@Override
	public String toString() {
        if (null == value) return getClass().getSimpleName() + ":null";
		if(value instanceof Double) return getClass().getSimpleName() + ":" + ((Double)value).toString();
		if(value instanceof Number) return getClass().getSimpleName() + ":" + ((Number)value).toString();
		if(value instanceof String) return getClass().getSimpleName() + ":" + ((String)value);
		return getClass().getSimpleName() + ":" + value;
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
	public boolean postAnalyze(Scope scope) {
		try {
			if (value instanceof Number) {
				VarType type = getType(scope);
				if (type == VarType.UNKNOWN) {
					markError("Unsupported numeric literal type");
					return false;
				}
				// Проверка диапазона через VarType.checkRange
				type.checkRange((Number)value);
			}
			return true;
		} 
		catch (SemanticException e) {
			markError(e.getMessage());
			return false;
		}
	}
	
	@Override
	public void codeGen(CodeGenerator cg) throws Exception {
		VarType type = getType(null);
//		if(VarType.CSTR == type) {
//			int resId = cg.defineData(new Operand(VarType.CSTR, OperandType.CONSTANT, value));
//			cg.setAcc(resId);
//		}
//		else
		if(VarType.NULL == type) {
			cg.setAcc(0x01, 0);
		}
		else {
			if(value instanceof Character) {
				cg.setAcc(0x01, (int)(Character)value);
			}
			else {
				cg.setAcc(type.getSize(), ((Number)value).longValue());
			}
		}
	}
}