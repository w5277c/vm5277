/*
 * Copyright 2026 konstantin@5277.ru
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

.IF OS_FT_TIMER == 0x01
.IFNDEF J8BPROC_UPTIME_SHR8
;-----------------------------------------------------------
J8BPROC_UPTIME_SHR8:
;-----------------------------------------------------------
;Загружаем в аккумулятор старшие 4 байта UPTIME
;OUT: ACCUM_EH/EL/H/L-uptime в миллисекундах/256
;-----------------------------------------------------------
	PUSH_Z

	LDI_Z _OS_UPTIME+0x01
	CLI
	LD ACCUM_L,Z+
	LD ACCUM_H,Z+
	LD ACCUM_EL,Z+
	LD ACCUM_EH,Z
	SEI

	POP_Z
	RET
.ENDIF
.ENDIF