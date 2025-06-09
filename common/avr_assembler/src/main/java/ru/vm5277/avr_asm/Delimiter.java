/*--------------------------------------------------------------------------------------------------------------------------------------------------------------
Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
----------------------------------------------------------------------------------------------------------------------------------------------------------------
22.04.2025	konstantin@5277.ru		Начало
28.04.2025	konstantin@5277.ru		Добавлен RANGE
09.06.2025	konstantin@5277.ru		Упрощен
--------------------------------------------------------------------------------------------------------------------------------------------------------------*/
package ru.vm5277.avr_asm;

public enum Delimiter {
    LEFT_PAREN('('),
    RIGHT_PAREN(')'),
    COMMA(',');
    
	private	final	char	symbol;

    Delimiter(char symbol) {
        this.symbol = symbol;
    }

	@Override
	public String toString() {
		return String.valueOf(symbol);
	}
	
    public static Delimiter fromSymbol(char symbol) {
        // Сначала проверяем многозначные разделители
        for (Delimiter delim : values()) {
            if (delim.symbol==symbol) {
				return delim;
            }
        }
        return null;
    }
}