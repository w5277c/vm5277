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
.include "./core/wait_ms.inc"

.IFNDEF C5_WAIT
;--------------------------------------------------------
C5_WAIT:
;--------------------------------------------------------
;Ждем 5ms
;--------------------------------------------------------
	PUSH ACCUM_H
	PUSH ACCUM_L

	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x05
	MCALL WAIT_MS

	POP ACCUM_L
	POP ACCUM_H
	RET
.ENDIF
