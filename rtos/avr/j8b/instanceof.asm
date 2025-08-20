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

.IFNDEF J8BPROC_INSTANCEOF
;-----------------------------------------------------------
J8BPROC_INSTANCEOF:
;-----------------------------------------------------------
;Проверяем реализацию интерфейса(перебор VarType ids)
;НЕ ВОССТАНАВЛИВАЕТ Z
;IN: Z-адрес HEAP,ACCUM_H-VarType id
;-----------------------------------------------------------
    PUSH TEMP_L
    ADIW ZL,0x03
    LD ACCUM_L,Z+
_J8BPROC_INSTANCEOF__LOOP:
    LD TEMP_L,Z+
    CP TEMP_L,ACCUM_H
    BREQ _J8BPROC_INSTANCEOF__OK
    DEC ACCUM_L
    BRNE _J8BPROC_INSTANCEOF__LOOP
    RJMP _J8BPROC_INSTANCEOF_END
_J8BPROC_INSTANCEOF__OK:
    LDI ACCUM_L,0x01
_J8BPROC_INSTANCEOF_END:
    POP TEMP_L
    RET
.ENDIF

