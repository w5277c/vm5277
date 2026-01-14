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

package ru.vm5277.flasher;

import ru.vm5277.common.firmware.Segment;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import ru.vm5277.common.DatatypeConverter;
import ru.vm5277.common.Platform;
import ru.vm5277.common.firmware.IntelHex.IntelHexParser;

public class FwCacher {

	public static boolean updateByCache(Path _cachePath, Platform _platform, int _signature, byte[] _uid, List<Segment> _segments, byte[] _data,
										int _maxFlashSize) {
		File file = _cachePath.resolve(buildFilename(_platform, _signature, _uid)).normalize().toFile();
		if(file.exists()) {
			try (Scanner scan = new Scanner(file)) {
				IntelHexParser ihp = new IntelHexParser(scan);
				byte[] cData = new byte[_maxFlashSize];
				List<Segment> cSegments = ihp.parse(cData);	
				
				for(Segment segment : _segments) {
					l1:
					for(Segment cSegment : cSegments) {
						if(cSegment.getAddr()<=segment.getAddr() && (cSegment.getAddr()+cSegment.getSize())>=(segment.getAddr()+segment.getSize())) {
							boolean isEquals = true;
							for(int i=0; i<segment.getSize(); i++) {
								if(_data[segment.getAddr()+i] != cData[cSegment.getAddr()+i]) {
									isEquals = false;
									break;
								}
							}
							if(isEquals) {
								segment.setModified(false);
							}
							break l1;
						}
					}
				}
				return true;
			}
			catch(Exception ex) {
				return false;
			}
		}
		return false;
	}

	static boolean clearCache(Path _cachePath, Platform _platform, int _signature, byte[] _UId) {
		File file = _cachePath.resolve(buildFilename(_platform, _signature, _UId)).toFile();
		if(file.exists()) {
			return file.delete();
		}
		return true;
	}

	static boolean putCache(Path _cachePath, Platform _platform, int _signature, byte[] _UId, byte[] _sourceData) {
		File file = _cachePath.resolve(buildFilename(_platform, _signature, _UId)).toFile();
		if(!file.exists()) {
			try {
				file.createNewFile();
			}
			catch(Exception ex) {
				return false;
			}
		}
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fos.write(_sourceData);
		}
		catch(Exception ex) {
			return false;
		}
		return true;
	}
	

	private static String buildFilename(Platform _platform, int _signature, byte[] _uid) {
		return	_platform.toString().toLowerCase() + "-" + String.format("%08X", _signature) + File.separator +
				DatatypeConverter.printHexBinary(_uid) + ".bin";
	}
}
