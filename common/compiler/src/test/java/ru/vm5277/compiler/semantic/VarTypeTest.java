/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.compiler.semantic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.CompileException;
import ru.vm5277.compiler.nodes.AstNode;

public class VarTypeTest {
    @Test
    void numericTypesCompatibility() throws CompileException {
        assertTrue(AstNode.isCompatibleWith(null, VarType.SHORT, VarType.BYTE));
		assertTrue(AstNode.isCompatibleWith(null, VarType.INT, VarType.BYTE));
		assertTrue(AstNode.isCompatibleWith(null, VarType.INT, VarType.SHORT));
		
        assertFalse(AstNode.isCompatibleWith(null, VarType.FIXED, VarType.BOOL));
		assertFalse(AstNode.isCompatibleWith(null, VarType.FIXED, VarType.BOOL));
		assertFalse(AstNode.isCompatibleWith(null, VarType.FIXED, VarType.BOOL));
		assertFalse(AstNode.isCompatibleWith(null, VarType.FIXED, VarType.BOOL));
    }

    @Test
    void arrayTypesCompatibility() throws CompileException {
        VarType intArray = VarType.arrayOf(VarType.INT);
        VarType anotherIntArray = VarType.arrayOf(VarType.INT);
        assertTrue(AstNode.isCompatibleWith(null, intArray, anotherIntArray));
    }
}