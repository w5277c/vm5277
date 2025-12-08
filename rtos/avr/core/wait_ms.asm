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
.IFNDEF WAIT_MS
WAIT_MS:
;--------------------------------------------------------
;Ждем истечения времени с момента прошлого сна или
;с момента вызова C5_TIMER_MARK
;Или выжидаем считая такты при OS_FT_TIMER1==0 (без
;мультипоточности)
;IN ACCUM_H,ACCUM_L - время в 0.001s
;--------------------------------------------------------
	PUSH ACCUM_H
	PUSH ACCUM_L
	PUSH_T16
.IF OS_FT_TIMER1 == 0x01
	LDS TEMP_H,_OS_UPTIME+0x05
_WAIT_MS__LOOP:
	LDS TEMP_L,_OS_UPTIME+0x05
	CP TEMP_H,TEMP_L
	BREQ _WAIT_MS__LOOP
	LDS TEMP_L,_OS_UPTIME+0x05
	SUB ACCUM_L,TEMP_L
	SBC ACCUM_H,C0x00
	BRCC _WAIT_MS__LOOP
.ELSE
_WAIT_MS__LOOP1:
	LDI TEMP_H,(29*CORE_FREQ)/10
_WAIT_MS__LOOP2:
	LDI TEMP_L,0x4f
_WAIT_MS__LOOP3:
	NOP
	DEC TEMP_L
	BRNE _WAIT_MS__LOOP3
	DEC TEMP_H
	BRNE _WAIT_MS__LOOP2

	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRCC _WAIT_MS__LOOP1
.ENDIF

	POP_T16
	POP ACCUM_L
	POP ACCUM_H
	RET
.ENDIF
