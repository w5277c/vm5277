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
package ru.vm5277.common.compiler;

import java.util.Map;
import ru.vm5277.common.cg.DataSymbol;
import static ru.vm5277.common.compiler.OperandType.ADDR_OFFSET;
import static ru.vm5277.common.compiler.OperandType.LOCAL_RESID;

public class Operand {
	private	VarType		varType;
	private	OperandType	opType;
	private	Object		value;
	private	Integer		resId;
	
	public Operand(VarType varType, OperandType opType, Object value) {
		this.varType = varType;
		this.opType = opType;
		this.value = value;
	}
	
	public Operand(VarType varType, OperandType opType) {
		this.varType = varType;
		this.opType = opType;
		this.value = null;
	}

	public VarType getVarType() {
		return varType;
	}
	
	public OperandType getOperandType() {
		return opType;
	}	
	
	public void setResId(int resId)  {
		this.resId = resId;
	}
	public Integer getResId() {
		return resId;
	}
	
	public Object getValue() {
		return value;
	}
	
/*	public String[] getParams(int size, Map<Integer, DataSymbol> flashData) throws Exception {
		String[] result = new String[size];
		
		switch (opType) {
			case ADDR_OFFSET:
			case LITERAL:
				long numValue = 0;
				if(value instanceof Number) {
					numValue = ((Number)value).longValue();
				}
				else if(value instanceof Character) {
					numValue = ((Character)value);
				}
				else throw new Exception("CG: literal must be a number, operand: " + toString());
				
				for(int i=0; i<size; i++) {
					result[i] = Long.toString(numValue&0xff);
					numValue >>>= 8;
				}
				break;
			case LOCAL_RESID:
				if(0x02 != size) throw new Exception("CG: incorrect size, operand: " + toString());
				
				int resId = (int)value;
				String strValue = flashData.get(resId).getLabel();
				result[0x00] = "low(" + strValue + ")";
				result[0x01] = "high(" + strValue + ")";
				break;
			default:
				throw new Exception("CG: unsupported operand type: " + opType);
		}
		return result;
	}
	*/
	
	@Override
	public String toString() {
		return opType + "[id:" + varType + "]" + (null == value ? "" : "=" + value); //TODO экранировать символы типа \n
	}
	
}
