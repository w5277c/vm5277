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

.IFNDEF OS_MULZ8
;--------------------------------------------------------
OS_MULZ8:
;--------------------------------------------------------
;Умножение регистра Z(16b) на 8
;IN: Z
;OUT: Z
;--------------------------------------------------------
	LSL ZL
	ROL ZH
	LSL ZL
	ROL ZH
	LSL ZL
	ROL ZH
	RET
.ENDIF
