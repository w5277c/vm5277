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
;IN ACCUM_H,ACCUM_L - время в 0.001s
;--------------------------------------------------------
	PUSH ACCUM_H
	PUSH ACCUM_L
	PUSH_T16

	LDS TEMP_H,_OS_UPTIME+0x05
_WAIT_MS__LOOP:
	LDS TEMP_L,_OS_UPTIME+0x05
	CP TEMP_H,TEMP_L
	BREQ _WAIT_MS__LOOP
	LDS TEMP_L,_OS_UPTIME+0x05
	SUB ACCUM_L,TEMP_L
	SBC ACCUM_H,C0x00
	BRCC _WAIT_MS__LOOP

	POP_T16
	POP ACCUM_L
	POP ACCUM_H
	RET
.ENDIF
