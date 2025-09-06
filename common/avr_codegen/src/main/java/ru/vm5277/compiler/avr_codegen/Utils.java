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
package ru.vm5277.compiler.avr_codegen;

public class Utils {

	/**
	 * brInstrInvert - инвертируем br инструкцию
	 * @param branchInstr - оригинальная br инструкция
	 * @return - инвертированная br инструкция, null если инструкции нет
	 */
	public static String brInstrInvert(String branchInstr) {
		switch(branchInstr.toLowerCase()) {
			case "brbc": return "brbs";
			case "brbs": return "brbc";
			case "brcc": return "brcs";
			case "brcs": return "brcc";
			case "breq": return "brne";
			case "brge": return "brlt";
			case "brhc": return "brhs";
			case "brhs": return "brhc";
			case "brid": return "brie";
			case "brie": return "brid";
			case "brlo": return "brcc";
			case "brlt": return "brge";
			case "brmi": return "brpl";
			case "brne": return "breq";
			case "brpl": return "brmi";
			case "brsh": return "brcs";
			case "brtc": return "brts";
			case "brts": return "brtc";
			case "brvc": return "brvs";
			case "brvs": return "brvc";
			default: return null;
		}
	}
}
