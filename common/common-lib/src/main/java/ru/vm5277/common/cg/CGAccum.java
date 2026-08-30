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
package ru.vm5277.common.cg;

import ru.vm5277.common.VarType;

public class CGAccum {
	private	int		refBytes;
	private	int		oldBits		= 0;	// Старый размер
	private	int		bits		= 8;	// Текущий размер в битах
//	private	int		resultBytes	= 0;	
	private	boolean	oldFixed;
	private	boolean	fixed;
	
	public CGAccum(int refBytes) {
		this.refBytes = refBytes;
	}
	
	public void setSize(VarType varType) {
		oldBits = bits;
		oldFixed = fixed;
		
		if(varType.isFixedPoint()) {
			fixed = true;
		}
		else {
			bits = (-1==varType.getSize() ? refBytes : varType.getSize())*8;
			fixed = false;
		}
	}
	
	public void setBits(int bits) {
		oldBits = bits;
		oldFixed = fixed;

		this.bits = bits;
		this.fixed = false;
	}
	public int getBits() {
		return bits;
	}
	public void setBytes(int bytes) {
		oldBits = bits;
		oldFixed = fixed;

		this.bits = (-1==bytes ? refBytes*8 : bytes*8);
		this.fixed = false;
	}
	public int getBytes() {
		int result = (bits+7)/8;
		return result < 5 ? result : 4;
	}

	public void setFixed() {
		oldBits = bits;
		oldFixed = fixed;

		bits = 16;
		fixed = true;
	}
	public boolean getFixed() {
		return fixed;
	}
	
	
	public boolean getOldFixed() {
		return oldFixed;
	}
	public int getOldBytes() {
		int result = (oldBits+7)/8;
		return result < 5 ? result : 4;
	}
	
/*	public void setResult(VarType varType) {
		resultBytes = (-1==varType.getSize() ? refBytes : varType.getSize());
	}
	public void setResult(int resultBytes) {
		this.resultBytes = (-1==resultBytes ? refBytes : resultBytes);
	}
	public int getResult() {
		return resultBytes;
	}*/
	
	public boolean isChanged() {
		return fixed!=oldFixed || bits!=oldBits;
	}
	
	public String oldToString() {
		return "acc" + (oldFixed ? "[FIXED]" : "") + " " + oldBits;
	}
	
	@Override
	public CGAccum clone() {
		CGAccum result = new CGAccum(refBytes);
		if(fixed) {
			result.setFixed();
		}
		else {
			result.setBits(bits);
		}
		return result;
	}
	
	@Override
	public String toString() {
		return "acc" + (fixed ? "[FIXED]" : "") + " " + bits;
		//return "acc" + (isFixed ? "[FIXED," : "") + " " + bits + "/" + resultBytes*8 + "] ";
	}
}
