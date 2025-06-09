/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.util.HashMap;
import ru.vm5277.common.exceptions.ParseException;

public class CodeSegment implements Segment {
	private	final	int					size;
	private	HashMap<Integer,CodeBlock>	blocksByStartAddr		= new HashMap<>();
	private	CodeBlock					curBlock				= null;
	
	public CodeSegment(int size) throws ParseException {
		if(0>size) throw new ParseException("TODO размер памяти не может быть отрицательным", null);
		this.size = size;
	}
	
	public void setAddr(Long addr) throws ParseException {
		if(null == addr || 0>addr || addr>size) throw new ParseException("TODO адрес за пределами flash памяти", null);
		
		// Если список пуст, создаем певый блок
		if(null == curBlock) {
			curBlock = new CodeBlock(addr.intValue());
			blocksByStartAddr.put(curBlock.getAddress(), curBlock);
			return;
		}

		CodeBlock db = null;
		for(CodeBlock _db : blocksByStartAddr.values()) {
			if(_db.getStartAddr() <= addr && (_db.getStartAddr()+_db.getLength()) > addr) {
				db = _db;
				break;
			}
		}
		if(null != db) {
			// Адрес находится в существующем блоке
			curBlock = db;
			curBlock.setAddr(addr.intValue());
		}
		else if((curBlock.getStartAddr() + curBlock.getLength()) == addr) {
			// Адрес вне блоков, но находится сразу за этим блоком(расширим блок)
			curBlock.setAddr(addr.intValue());
		}
		else {
			// Адрес вне блоков, создаем новый блок
			curBlock = new CodeBlock(addr.intValue());
			blocksByStartAddr.put(curBlock.getAddress(), curBlock);
		}
	}
	
	public int writeOpcode(int opcode) {
		int result = curBlock.getPC();
		curBlock.writeOpcode(opcode);
		return result;
	}

	@Override
	public CodeBlock getCurrentBlock() {
		return curBlock;
	}
}
