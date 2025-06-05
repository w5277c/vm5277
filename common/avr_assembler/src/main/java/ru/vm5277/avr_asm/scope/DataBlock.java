/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
04.06.2025	konstantin@5277.ru		Взято из проекта javra
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm.scope;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataBlock {
	protected	int			start;
	protected	int			offset	= 0;
	protected	int			length	= 0;
	protected	byte[]		data		= new byte[0x40];
	protected	int			overlap	= 0;
	
	public DataBlock(int start) {
		this.start = start;
	}

	public void allocate(int size) {
		offset += size;
		if(offset > length) {
			length = offset;
		}
	}
	public void writeOpcode(int opcode) {
		ByteBuffer bb = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(opcode);
		write(bb.array(), 0x02);
	}
	
	public void write(byte[] bdata, int size) {
		if((this.data.length - offset) < size) {
			byte[] _data = new byte[offset + size + 0x40];
			System.arraycopy(this.data, 0x00, _data, 0x00, offset);
			this.data = _data;
		}
		System.arraycopy(bdata, 0x00, this.data, offset, size);
		offset += size;
		if(offset > length) {
			length = offset;
		}
	}
	
	public int getAddress() {
		return start+offset;
	}
	public void setAddr(int addr) {
		offset = addr-start;
	}

	public int getLength() {
		return length;
	}

	public int getStart() {
		return start;
	}

	public void setOverlap(int addr) {
		int delta = (start+length-0x01)-addr;
		overlap = (0<delta ? delta : 0);
	}

	public int getOverlap() {
		return overlap;
	}
	
	public byte[] getData() {
		return data;
	}

	public void append(DataBlock block) {
		byte _data[] = new byte[length + block.getLength()];
		System.arraycopy(data, 0x00, _data, 0x00, length);
		System.arraycopy(block.getData(), 0x00, _data, length, block.getLength());
		data = _data;
		length = data.length;
	}
}
