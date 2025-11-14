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

import ru.vm5277.common.cg.CGCells;
import ru.vm5277.common.cg.DataSymbol;
import ru.vm5277.common.compiler.VarType;


public abstract class CGCellsScope extends CGScope {
	protected	final	VarType		type;
	protected	final	int			size;
	protected			DataSymbol	dataSymbol;
	protected			boolean		isConstant;
	protected			boolean		isArrayView;
	
	public CGCellsScope(CGScope parent, int resId, VarType type, int size, String name) {
		super(parent, resId, name);
		this.type = type;
		this.size = size;
	}
	
	public abstract CGCells getCells();
	
	public void setDataSymbol(DataSymbol dataSymbol) {
		this.dataSymbol = dataSymbol;
	}
	public DataSymbol getDataSymbol() {
		return dataSymbol;
	}

	public VarType getType() {
		return type;
	}
	
	public int getSize() {
		return size;
	}
	
	public boolean isConstant() {
		return isConstant;
	}
	
	public void setArrayView(boolean isArrayView) {
		this.isArrayView = isArrayView;
	}
	public boolean isArrayView() {
		return isArrayView;
	}
}
