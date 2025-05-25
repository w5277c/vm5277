/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
25.05.2025	konstantin@5277.ru		Начало
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.j8b.compiler.common;

public class Case {
	private	final	long	from;
	private	final	Long	to;
	private	final	int		blockId;
	
	public Case(long from, Long to, int blockId) {
		this.from = from;
		this.to = to;
		this.blockId = blockId;
	}
	
	public long getFrom() {
		return from;
	}
	
	public Long getTo() {
		return to;
	}
	
	public int getBlockId() {
		return blockId;
	}
	
	@Override
	public String toString() {
		return (null == to ? from : from + "-" + to) + ":" + blockId;
	}
}
