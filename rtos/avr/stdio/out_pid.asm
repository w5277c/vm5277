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

.IF OS_FT_STDOUT == 0x01
.include "stdio/out_hex16.asm"

.IFNDEF OS_OUT_PID
;--------------------------------------------------------
OS_OUT_PID:
;--------------------------------------------------------
;Вывод идентификатора процеса (PID-2байта)
;--------------------------------------------------------
	PUSH_A16

	MOVW ACCUM_L,PID_L
	MCALL OS_OUT_HEX16

	POP_A16
	RET
.ENDIF
.ENDIF
