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

package ru.vm5277.common.firmware.IntelHex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import ru.vm5277.common.Pair;
import ru.vm5277.common.firmware.Segment;

public class IntelHexParser implements Iterator<IntelHexRow>{
	private  Scanner  scan;

	public IntelHexParser(Scanner scan) {
		this.scan = scan;
	}

	@Override
	public boolean hasNext() {
		return scan.hasNextLine();
	}

	@Override
	public IntelHexRow next() {
		IntelHexRow result = null;
		try {
			result = new IntelHexRow(scan.nextLine());
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	public List<Segment> parse(byte[] flashData) throws Exception {
		List<Segment> segments = new ArrayList<>();
		
		while(hasNext()) {
			IntelHexRow ihr = next();
			if(null!=ihr) {
				if(EIntelRecordType.EXT_SEGMENT==ihr.getType()) {
					//START OF RECORD?
				}
				else if(EIntelRecordType.EOF==ihr.getType()) {
					break;
				}
				else if(EIntelRecordType.DATA==ihr.getType()) {
					if(ihr.getAddress()>=flashData.length) {
						throw new Exception("Incorrect flash size, current addr:" + Integer.toString(ihr.getAddress()));
					}
					if(0x00 != ihr.getLength()) {
						System.arraycopy(ihr.getData(), 0x00, flashData, ihr.getAddress(), ihr.getLength());

						appendSegment(ihr.getAddress(), ihr.getLength(), segments);
					}
				}
				else {
					throw new Exception("Unsupported record type:" + ihr.getType());
				}
			}
			else {
				throw new Exception("File parsing fault");
			}
		}
		return segments;
	}

	public void appendSegment(int address, int length, List<Segment> segments) {
		if(segments.isEmpty()) {
			segments.add(new Segment(address, length));
		}
		else {
			boolean appended = false;
			for(Segment segment : segments) {
				if(segment.getAddr() + segment.getSize()==address) {
					segment.setSize(segment.getSize() + length);
					appended = true;
					break;
				}
				else if(address + length==segment.getAddr()) {
					segment.setAddr(address);
					segment.setSize(segment.getSize() + length);
					appended = true;
					break;
				}
			}
			if(!appended) {
				segments.add(new Segment(address, length));
			}
		}
	}
}
