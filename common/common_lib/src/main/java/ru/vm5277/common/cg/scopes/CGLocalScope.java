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
package ru.vm5277.common.cg.scopes;

import ru.vm5277.common.cg.CGCell;
import ru.vm5277.common.cg.DataSymbol;

public class CGLocalScope extends CGScope {
	private	final	int						typeId;
	private	final	int						size;
	private			boolean					isConstant;
	private			int						stackOffset;
	private			DataSymbol				dataSymbol;
	private			CGCell[]				cells;
			
	public CGLocalScope(CGScope parent, int resId, int typeId, int size, boolean isConstant, String name, CGCell[] cells) {
		super(parent, resId, name);
		
		this.typeId = typeId;
		this.size = size;
		this.isConstant = isConstant;
		this.cells = cells;
	}
	
	public CGCell[] getCells() {
		return cells;
	}
	
	public void setDataSymbol(DataSymbol dataSymbol) {
		this.dataSymbol = dataSymbol;
	}
	public DataSymbol getDataSymbol() {
		return dataSymbol;
	}
	
	
	public void setStackOffset(int offset) {
		this.stackOffset = offset;
	}
	public int getStackOffset() {
		return stackOffset;
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
	
	public Integer getRegCellPos(byte reg) {
		for(int pos=0; pos<cells.length;pos++) {
			CGCell cell = cells[pos];
			if(CGCell.Type.REG == cell.getType() && cell.getNum() == reg) return pos;
		}
		return null;
	}
}
