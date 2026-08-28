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

.IFNDEF OS_UPTIME16
;-----------------------------------------------------------
OS_UPTIME16:
;-----------------------------------------------------------
;Загружаем в аккумулятор 2 первых байта UPTIME
;OUT: ACCUM_L/H-два первых байта UPTIME
;-----------------------------------------------------------
	PUSH TEMP_L
   	LDS TEMP_L,SREG
	CLI

	LDS ACCUM_L,_OS_UPTIME+0x00
	LDS ACCUM_H,_OS_UPTIME+0x01

	STS SREG,TEMP_L
	POP TEMP_L
	RET
.ENDIF