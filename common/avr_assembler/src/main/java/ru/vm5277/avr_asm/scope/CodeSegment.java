/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.util.HashMap;
import ru.vm5277.common.exceptions.ParseException;

public class CodeSegment {
	private			int							PCReg				= 0x0000;
	private	final	int							wSize;
	private			HashMap<Integer,CodeBlock>	blocksByStartAddr	= new HashMap<>();
	private			CodeBlock					curBlock			= null;
	
	public CodeSegment(int wSize) throws ParseException {
		if(0>wSize) throw new ParseException("TODO размер памяти не может быть отрицательным", null);
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
			if(_db.getStartWAddress() <= PCReg && (_db.getStartWAddress()+_db.getLength()/2) > PCReg) {
				db = _db;
				break;
			}
		}
		if(null != db) {
			// Адрес находится в существующем блоке
			curBlock = db;
			curBlock.setOffset(PCReg);
		}
		else if((curBlock.getStartWAddress() + curBlock.getLength()/2) == PCReg) {
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
}
