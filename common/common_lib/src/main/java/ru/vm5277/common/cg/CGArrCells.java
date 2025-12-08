package ru.vm5277.common.cg;

import ru.vm5277.common.VarType;
import ru.vm5277.common.exceptions.CompileException;


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

public class CGArrCells extends CGCells {
//	private	VarType		varType;
//	private	CGCells		varFieldCells;	//Значения динамических индексов помещаем в стек(для вычисления адреса используем функцию RTOS
	private	CGCells[]	indexesCells	= new CGCells[0x03];
	private	int[]		indexesConst	= new int[]{-1, -1, -1};
	private	int[]		dimensionsConst= new int[]{0, 0, 0};
	private	int			depth;
	
	public CGArrCells(int depth) {
		super(Type.ARRAY);
		
		this.depth = depth;
	}

	public void setIndexConst(int depthPos, int index) {
		indexesConst[depthPos] = index;
	}
	
	public void setIndexCells(int depthPos, CGCells cells) {
		indexesCells[depthPos] = cells;
	}
	
	public void setDimensionsConst(int[] dimensions) {
		this.dimensionsConst = dimensions;
	}

	public boolean canComputeStatic() {
		if(null==dimensionsConst) return false;
		for(int i=0; i<depth; i++) {
			if(0==dimensionsConst[i] || -1==indexesConst[i]) return false;
		}
		return true;
	}
	
	//Возможне только для полноценных массивов(не view, так как для view нужно в runtime получить адрес массива)
	public int computeStaticAddr() throws CompileException {
		int staticAddr = 0;
		switch(depth) {
			case 0x01:
				staticAddr = indexesConst[0];
				break;
			case 0x02:
				staticAddr = indexesConst[0] * dimensionsConst[1] + indexesConst[1];
				break;
			case 0x03:
				staticAddr = indexesConst[0] * dimensionsConst[1] * dimensionsConst[2] + indexesConst[1] * dimensionsConst[2] + indexesConst[2];
				break;
		}
		staticAddr *= size;
		staticAddr += (0x01+0x01+0x01+depth*0x02);
		return staticAddr;
	}
	
	public int computeStaticSize() throws CompileException {
		int staticSize = 0;
		switch(depth) {
			case 0x01:
				staticSize = dimensionsConst[0];
				break;
			case 0x02:
				staticSize = dimensionsConst[0] * dimensionsConst[1];
				break;
			case 0x03:
				staticSize = dimensionsConst[0] * dimensionsConst[1] * dimensionsConst[2];
				break;
		}
		staticSize *= size;
		staticSize += (0x01+0x01+0x01+depth*0x02);
		return staticSize;
	}
	
	public int getDepth() {
		return depth;
	}
}
