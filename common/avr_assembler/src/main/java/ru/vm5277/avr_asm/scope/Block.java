/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

public class Block {
	protected	final	static	int		PART_SIZE	= 0x80;
	protected					int		startAddr;
	protected					int		offset		= 0;
	protected					int		length;
	protected					byte[]	data		= new byte[PART_SIZE];
	
	public void write(byte[] bdata, int size) {
		if(data.length-offset < size) {
			byte[] _data = new byte[offset + size + PART_SIZE];
			System.arraycopy(data, 0x00, _data, 0x00, offset);
			data = _data;
		}
		System.arraycopy(bdata, 0x00, data, offset, size);
		offset += size;
		if(offset>length) length=offset;
	}

	public void append(CodeBlock block) {
		byte _data[] = new byte[offset + block.getLength()];
		System.arraycopy(data, 0x00, _data, 0x00, offset);
		System.arraycopy(block.getData(), 0x00, _data, offset, block.getLength());
		data = _data;
		offset = data.length;
		if(offset>length) length=offset;
	}

	public int getStartAddr() {
		return startAddr;
	}
	
	public int getAddress() {
		return startAddr+offset;
	}
	public void setAddr(int addr) {
		offset = addr-startAddr;
	}

	public int getLength() {
		return length;
	}
	
	public byte[] getData() {
		return data;
	}
}
