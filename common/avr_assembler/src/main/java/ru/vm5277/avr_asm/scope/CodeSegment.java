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
package ru.vm5277.avr_asm.scope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.common.firmware.FirmwareFormatter;

public class CodeSegment {
	private			int							PCReg				= 0x0000;
	private	final	int							wSize;
	private			HashMap<Integer,CodeBlock>	blocksByStartAddr	= new HashMap<>();
	private			CodeBlock					curBlock			= null;
	
	public CodeSegment(int wSize) throws CompileException {
		if(0>wSize) throw new CompileException("TODO размер памяти не может быть отрицательным", null);
		this.wSize = wSize;
	}

	public CodeBlock getCurrentBlock() {
		return curBlock;
	}
	
	public int getPC() {
		return PCReg;
	}
	public void setPC(int PCReg) {
		this.PCReg = PCReg;
		
		// Если список пуст, создаем певый блок
		if(null == curBlock) {
			curBlock = new CodeBlock(PCReg);
			blocksByStartAddr.put(curBlock.getStartWAddress(), curBlock);
			return;
		}

		CodeBlock db = null;
		for(CodeBlock _db : blocksByStartAddr.values()) {
			if(_db.getStartWAddress() <= PCReg && (_db.getStartWAddress()+_db.getWSize()) > PCReg) {
				db = _db;
				break;
			}
		}
		if(null != db) {
			// Адрес находится в существующем блоке
			curBlock = db;
			curBlock.setOffset(PCReg);
		}
		else if((curBlock.getStartWAddress() + curBlock.getWSize()) == PCReg) {
			// Адрес вне блоков, но находится сразу за этим блоком(расширим блок)
			curBlock.setOffset(PCReg);
		}
		else {
			// Адрес вне блоков, создаем новый блок
			curBlock = new CodeBlock(PCReg);
			blocksByStartAddr.put(PCReg, curBlock);
		}
	}
	public void movePC(int offset) {
		PCReg += offset;
	}
	
	public int getWSize() {
		return wSize;
	}
	
	public boolean isEmpty() {
		return blocksByStartAddr.isEmpty();
	}

	public void printStat() {
		int total = 0;
		System.out.println("--CODE-----------------------------------");
		for(Integer startAddr : blocksByStartAddr.keySet()) {
			CodeBlock block = blocksByStartAddr.get(startAddr);
			System.out.println( " Start\t= " + String.format("%04X", block.getStartWAddress()) +
								", End = " + String.format("%04X", block.getStartWAddress() + block.getWSize() - 0x01) +
								", Length = " + String.format("%04X", block.getWSize()) + 
								(0x00 == block.getOverlap() ? "" : " [Overlap: " + String.format("%04X", block.getOverlap()) + "]"));
			total += (block.getWSize() - block.getOverlap());
		}
		System.out.println(" -----");
		System.out.println(" Total\t:  " + total + " words (" + (total*2) + " bytes) " + String.format("%.1f", 100d*total/wSize) + "%");
	}

	public void build(FirmwareFormatter fwFormatter) throws IOException {
		List<CodeBlock> sorted = new ArrayList<>(blocksByStartAddr.values());
		Collections.sort(sorted, new Comparator<CodeBlock>() {
			@Override
			public int compare(CodeBlock cb1, CodeBlock cb2) {
				if(cb1.getStartWAddress() == cb2.getStartWAddress()) {
					return Integer.compare(cb1.getWSize(), cb2.getWSize());
				}
				return Integer.compare(cb1.getStartWAddress(), cb2.getStartWAddress());
			}
		});
		for(int index=0; index<(sorted.size()-0x01);index++) {
			CodeBlock cb1 = sorted.get(index);
			CodeBlock cb2 = sorted.get(index+0x01);
			if((cb1.getStartWAddress() + cb1.getWSize()-0x01) >= cb2.getStartWAddress()) {
				cb1.setOverlap(cb2.getStartWAddress());
			}
		}

		for(CodeBlock block : blocksByStartAddr.values()) {
			fwFormatter.push(block.getData(), block.getStartWAddress()*2, block.getWSize()*2 - block.getOverlap()*2);
		}
	}
}
