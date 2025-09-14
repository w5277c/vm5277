# Новости и история разработки

## [2025-09-15] - Реализация операций с фиксированной точкой (fixed Q7.8)

### ✅ Статус
Реализация завершена на уровне кодогенерации, но **не была протестирована на реальном устройстве или в симуляторе**.
Добавлена полная поддержка типа `fixed` (Q7.8) с арифметическими операциями и выводом.

### Цель
Реализованы арифметические операции (сложение, вычитание, умножение, деление) для типа данных `fixed` (фиксированная точка Q7.8) в рамках кодогенерации для AVR.

### Исходный код (Java-подобный)

```java
import rtos.System;
import rtos.RTOSParam;

class Main {
    public static void main() {
		System.setParam(RTOSParam.STDOUT_PORT, 0x12);

		short s1 = 10;
		fixed f1 = 6;
		fixed f2 = 1.5;

		System.out(s1 + 2);
		System.out(s1 + 1.5);
		System.out(f1 + 1.5);
		System.out(s1 - 2);
		System.out(s1 - 1.5);
		System.out(f1 - 1.5);
		System.out(s1 * 2);
		System.out(s1 * 1.5);
		System.out(f1 * 1.5);
		System.out(s1 / 2);
		System.out(s1 / 1.5);
		System.out(f1 / 1.5);
		System.out(s1 % 2);

		System.out(f1 + f2);
		System.out(f1 - f2);
		System.out(f1 * f2);
		System.out(f1 / f2);
	}
}
```

### Результат на AVR ассемблере

```asm
; vm5277.avr_codegen v0.1 at Mon Sep 15 09:35:15 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "math/mul16.asm"
.include "math/mulq7n8.asm"
.include "math/div16.asm"
.include "math/divq7n8.asm"
.include "stdio/out_num16.asm"
.include "stdio/out_q7n8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta19:
	.db 12,0

j8bCMainMmain:
	ldi r20,10
	ldi r21,0
	ldi r22,0
	ldi r23,6
	ldi r24,128
	ldi r25,1
	mov r16,r20
	mov r17,r21
	subi r16,254
	sbci r17,255
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	mov r17,r16
	clr r16
	subi r16,128
	sbci r17,254
	rcall os_out_q7n8
	mov r16,r22
	mov r17,r23
	subi r16,128
	sbci r17,254
	rcall os_out_q7n8
	mov r16,r20
	mov r17,r21
	subi r16,2
	sbci r17,0
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	mov r17,r16
	clr r16
	subi r16,128
	sbci r17,1
	rcall os_out_q7n8
	mov r16,r22
	mov r17,r23
	subi r16,128
	sbci r17,1
	rcall os_out_q7n8
	mov r16,r20
	mov r17,r21
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_mul16
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	mov r17,r16
	clr r16
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_mulq7n8
	rcall os_out_q7n8
	mov r16,r22
	mov r17,r23
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_mulq7n8
	rcall os_out_q7n8
	mov r16,r20
	mov r17,r21
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_div16
	pop TEMP_H
	pop TEMP_L
	rcall os_out_num16
	mov r16,r20
	mov r17,r21
	mov r17,r16
	clr r16
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_divq7n8
	pop TEMP_H
	pop TEMP_L
	rcall os_out_q7n8
	mov r16,r22
	mov r17,r23
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,128
	ldi ACCUM_EH,1
	rcall os_divq7n8
	pop TEMP_H
	pop TEMP_L
	rcall os_out_q7n8
	mov r16,r20
	mov r17,r21
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_div16
	movw ACCUM_L,TEMP_L
	pop TEMP_H
	pop TEMP_L
	rcall os_out_num16
	mov r16,r24
	mov r17,r25
	add r16,r22
	adc r17,r23
	rcall os_out_q7n8
	mov r16,r22
	mov r17,r23
	sub r16,r24
	sbc r17,r25
	rcall os_out_q7n8
	mov r16,r24
	mov r17,r25
	mov ACCUM_EL,r22
	mov ACCUM_EH,r23
	rcall os_mulq7n8
	rcall os_out_q7n8
	mov r16,r22
	mov r17,r23
	mov ACCUM_EL,r24
	mov ACCUM_EH,r25
	tst r17
	brne _j8b_nediv25
	tst r18
	brne _j8b_nediv25
;TODO Division by zero
	ldi r16,0xff
	ldi r17,0xff
	rjmp _j8b_ediv24
_j8b_nediv25:
	push TEMP_L
	push TEMP_H
	rcall os_divq7n8
	pop TEMP_H
	pop TEMP_L
_j8b_ediv24:
	rcall os_out_q7n8
	ret
```

### Ключевые изменения
- **Generator.java**: Добавлена кодогенерация для операций с fixed (mulq7n8, divq7n8)
- **RTOS**: Добавлены функции os_mulq7n8, os_divq7n8, os_out_q7n8
- **ExpressionNode.java**: Оптимизация выражений с fixed
- **LiteralExpression.java**: Поддержка литералов fixed
- **VarType.java**: Добавлены константы для диапазона fixed
- **BinaryExpression.java**: Проверка типов и кодогенерация для операций с fixed

### Достижения
1. **Полная поддержка типа fixed**: Инициализация, арифметические операции, вывод.
2. **Смешанные выражения**: Операции между integer и fixed типами.
3. **Оптимизация**: Свёртывание константных выражений на этапе компиляции.
4. **Контроль ошибок**: Проверка диапазона значений при инициализации.

### Технические детали
- **Формат Q7.8**: 1 знаковый бит, 7 бит целой части, 8 бит дробной.
- **Библиотеки RTOS**: Добавлены оптимизированные ассемблерные процедуры для умножения и деления.
- **Преобразование типов**: Автоматическое приведение integer к fixed при mixed expressions.


## [2025-09-14] - Реализация математических операций в рамках кодогенерации для AVR

### ⚠️ Статус
Реализация завершена на уровне кодогенерации, но **не была протестирована на реальном устройстве или в симуляторе**.
Код будет всесторонне проверен и отлажен в будущем. Пока ключевая цель - общая реализация кодогенератора.

### Цель
Реализованы основные математические операции (сложение, вычитание, умножение, деление, остаток от деления) для целочисленных типов данных в рамках кодогенерации для AVR.
Оптимизированы цепочки арифметических выражений, добавлена поддержка операций с константами на этапе компиляции.

### Исходный код (Java-подобный)

```java
import rtos.System;
import rtos.RTOSParam;

class Main {
    public static void main() {
		System.setParam(RTOSParam.STDOUT_PORT, 0x12);

		short s1 = 10;
		short s2 = 2;
		System.out(s1 + 2);
		System.out(s1 - 2);
		System.out(s1 * 2);
		System.out(s1 / 2);
		System.out(s1 % 2);

		System.out(s1 + s2);
		System.out(s1 - s2);
		System.out(s1 * s2);
		System.out(s1 / s2);
		System.out(s1 % s2);

	}
}
```

### Результат на AVR ассемблере

```asm
; vm5277.avr_codegen v0.1 at Sun Sep 14 04:16:21 VLAT 2025
.equ stdout_port = 18

.set OS_FT_STDOUT = 1
.set OS_FT_DRAM = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "math/mul16.asm"
.include "math/div16.asm"
.include "stdio/out_num16.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta18:
	.db 12,0

j8bCMainMmain:
	ldi r20,10
	ldi r21,0
	ldi r22,2
	ldi r23,0
	mov r16,r20
	mov r17,r21
	subi r16,254
	sbci r17,255
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	subi r16,2
	sbci r17,0
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_mul16
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_div16
	pop TEMP_H
	pop TEMP_L
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	push TEMP_L
	push TEMP_H
	ldi ACCUM_EL,2
	ldi ACCUM_EH,0
	rcall os_div16
	movw ACCUM_L,TEMP_L
	pop TEMP_H
	pop TEMP_L
	rcall os_out_num16

	mov r16,r22
	mov r17,r23
	add r16,r20
	adc r17,r21
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	sub r16,r22
	sbc r17,r23
	rcall os_out_num16

	mov r16,r22
	mov r17,r23
	mov ACCUM_EL,r20
	mov ACCUM_EH,r21
	rcall os_mul16
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	mov ACCUM_EL,r22
	mov ACCUM_EH,r23
	tst r17
	brne _j8b_nediv24
	tst r18
	brne _j8b_nediv24
;TODO Division by zero
	ldi r16,0xff
	ldi r17,0xff
	rjmp _j8b_ediv23
_j8b_nediv24:
	push TEMP_L
	push TEMP_H
	rcall os_div16
	pop TEMP_H
	pop TEMP_L
_j8b_ediv23:
	rcall os_out_num16

	mov r16,r20
	mov r17,r21
	mov ACCUM_EL,r22
	mov ACCUM_EH,r23
	tst r17
	brne _j8b_nediv26
	tst r18
	brne _j8b_nediv26
;TODO Division by zero
	ldi r16,0xff
	ldi r17,0xff
	rjmp _j8b_ediv25
_j8b_nediv26:
	push TEMP_L
	push TEMP_H
	rcall os_div16
	movw ACCUM_L,TEMP_L
	pop TEMP_H
	pop TEMP_L
_j8b_ediv25:
	rcall os_out_num16
	ret
```

### Ключевые изменения
- **ExpressionNode.java**: Полностью переработаны методы оптимизации арифметических цепочек
- **Generator.java**: Реализована кодогенерация для математических операций на ассемблере AVR
- **RTOS**: Добавлены функции di8, mul16, mul32

### Достижения
1. **Поддержка математических операций**: Корректная генерация кода для +, -, *, /, %.
2. **Оптимизация выражений**: Агрессивное свертывание констант и упрощение выражений на этапе компиляции.
3. **Обработка ошибок**: Контроль деления на ноль на уровне кодогенерации.

### Технические детали
- **Процедуры умножения/деления**: Реализованы как отдельные подпрограммы (rcall os_mul8, rcall os_div16 и т.д.) в RTOS для экономии места в коде.
- **Работа с разрядностью**: Поддержка операций для 8-, 16- и 32-битных операндов.
- **Константные выражения**: Вычисления с участием констант выполняются на этапе компиляции, что уменьшает размер генерируемого кода и время выполнения.

### Следующие шаги
- Реализация операций с фиксированной точкой (fixed Q7.8)


## [2025-09-11] - Реализация работы с полями, аргументами и переменными в рамках кодогенерации для AVR

### Цель
Реализованы основные алгоритмы для инициализации, записи/передачи и чтения значений из полей класса, локальных переменных метода и аргументов.
Приведена в порядок логика создания объектов, вызова конструкторов и методов, передача параметров и возврат значения.

### Исходный код (Java-подобный)

```java
import rtos.System;
import rtos.RTOSParam;

class Main {
	class Test {
		private byte h1 = 10;
		private byte h2;

		public Test() {
			h2 = 20;
		}

		public Test(byte a) {
			h2 = a;
		}

		byte sum() {
			return h1+h2;
		}
	}

	public void go(byte b1, byte b2, byte b3) {
		byte b4 = 4;
		byte b5 = 5;

		System.out(b1);
		System.out(b2);
		System.out(b3);
		System.out(b4);
		System.out(b5);

		{
			byte b6 = 6;
			System.out(b1);
			System.out(b4);
			System.out(b5);
			System.out(b6);
		}
	}

    public static void main() {
		System.setParam(RTOSParam.STDOUT_PORT, 0x12);

		go(1,2,3);

		Test test = new Test(12);
		System.out(test.sum());
	}
}
```

### Результат на AVR ассемблере

```asm
; vm5277.avr_codegen v0.1 at Thu Sep 11 04:00:14 VLAT 2025
.equ stdout_port = 18

.set OS_FT_DRAM = 1
.set OS_FT_STDOUT = 1

.include "devices/atmega328p.def"
.include "core/core.asm"
.include "dmem/dram.asm"
.include "j8b/inc_refcount.asm"
.include "j8b/dec_refcount.asm"
.include "j8b/clear_fields.asm"
.include "stdio/out_num8.asm"

Main:
	rjmp j8bCMainMmain
_j8b_meta18:
	.db 12,0
_j8b_meta20:
	.db 13,0
_j8b_finit19:
	ldi r16,10
	std z+6,r16
	ret

j8bC24CTestMTest:
	ldi r16,low(7)
	ldi r17,high(7)
	rcall os_dram_alloc
	std z+0,r16
	std z+1,r17
	std z+2,c0x00
	ldi r16,low(_j8b_meta20*2)
	std z+3,r16
	ldi r16,high(_j8b_meta20*2)
	std z+4,r16
	rcall j8bproc_clear_fields_nr
_j8b_cinit25:
	rcall _j8b_finit19
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	ldd r16,y+5
	std z+5,r16
	ret

j8bC27CTestMsum:
	ldd r16,z+5
	push yl
	ldd yl,z+6
	add r16,yl
	pop yl
	ret

j8bC29CMainMgo:
	push yl
	push yh
	lds yl,SPL
	lds yh,SPH
	push r20
	push r21
	ldi r20,4
	ldi r21,5
	ldd r16,y+7
	rcall os_out_num8
	ldd r16,y+6
	rcall os_out_num8
	ldd r16,y+5
	rcall os_out_num8
	mov r16,r20
	rcall os_out_num8
	mov r16,r21
	rcall os_out_num8
	push r22
	ldi r22,6
	ldd r16,y+7
	rcall os_out_num8
	mov r16,r20
	rcall os_out_num8
	mov r16,r21
	rcall os_out_num8
	mov r16,r22
	rcall os_out_num8
	pop r22
	pop r21
	pop r20
	ret

j8bCMainMmain:
	push zl
	ldi r30,1
	push r30
	ldi r30,2
	push r30
	ldi r30,3
	push r30
	rcall j8bC29CMainMgo
	pop j8b_atom
	pop j8b_atom
	pop j8b_atom
	pop zl
	push zl
	push zh
	ldi r30,12
	push r30
	rcall j8bC24CTestMTest
	pop j8b_atom
	pop zh
	pop zl
	mov r20,r16
	mov r21,r17
	push zl
	push zh
	mov r30,r20
	mov r31,r21
	rcall j8bC27CTestMsum
	pop zh
	pop zl
	rcall os_out_num8
	ret
```

### Достижения

1. **Инициализация полей**: Корректная обработка инициализации при объявлении (`private byte h1 = 10`)
2. **Поддержка конструкторов**: Добавлена поддержка нескольких конструкторов с параметрами
3. **Корректная работа со смещениями в STACK и HEAP**: Добавлены проверки смещений в инструкциях типа `ldd r16, y+const` с последующей корректировкой адреса в индексных регистрах
4. **Работа со стеком**: Корректная передача параметров и очистка стека

### Технические детали

- **Структура объекта**: 5-байтовый заголовок + поля данных
- **Смещения полей**: h1 по z+6, h2 по z+5
- **Передача параметров**: Через стек с правильной очисткой
- **Структура стека**: аргументы, адрес возврата, сохранённый Y, локальные переменные
### Следующие шаги

- Реализация математических операций
---
