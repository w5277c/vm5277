/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.j8b.compiler.semantic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import ru.vm5277.j8b.compiler.enums.VarType;

public class VarTypeTest {
    @Test
    void numericTypesCompatibility() {
        assertTrue(VarType.SHORT.isCompatibleWith(VarType.BYTE));
		assertTrue(VarType.INT.isCompatibleWith(VarType.BYTE));
		assertTrue(VarType.INT.isCompatibleWith(VarType.SHORT));
		
        assertFalse(VarType.FIXED.isCompatibleWith(VarType.BOOL));
		assertFalse(VarType.FIXED.isCompatibleWith(VarType.BOOL));
		assertFalse(VarType.FIXED.isCompatibleWith(VarType.BOOL));
		assertFalse(VarType.FIXED.isCompatibleWith(VarType.BOOL));
    }

    @Test
    void arrayTypesCompatibility() {
        VarType intArray = VarType.arrayOf(VarType.INT);
        VarType anotherIntArray = VarType.arrayOf(VarType.INT);
        assertTrue(intArray.isCompatibleWith(anotherIntArray));
    }
}