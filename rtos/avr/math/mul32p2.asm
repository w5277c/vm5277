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

.IFNDEF OS_MUL32P2
;-----------------------------------------------------------
;Умножение 32бит числа на степень двойки(на базе метки),
;Не сообщает об переполнении
;IN: ACCUM_L/H/EL/EH-32b число, RESULT должен быть = 0!
;OUT: ACCUM_L/H/EL/EH-16b результат
;-----------------------------------------------------------
OS_MUL32P2_X128:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01
OS_MUL32P2_X64:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01
OS_MUL32P2_X32:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01
OS_MUL32P2_X16:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01
OS_MUL32P2_X8:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01
OS_MUL32P2_X4:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01
OS_MUL32P2_X2:
	LSL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	BRCC PC+0x02
	ORI RESULT,0x01

	LSR RESULT
	RET
.ENDIF
