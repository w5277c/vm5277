/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.compiler.semantic;

import ru.vm5277.compiler.semantic.Symbol;
import ru.vm5277.compiler.semantic.ClassScope;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import ru.vm5277.common.compiler.VarType;
import ru.vm5277.common.exceptions.SemanticException;

public class ScopeTest {
    private ClassScope globalScope;
    private ClassScope childScope;

    @BeforeEach
    void setup() throws SemanticException {
        globalScope = new ClassScope("Global", null);
        childScope = new ClassScope("Child", globalScope);
    }

    @Test
    void resolveFieldFromParentScope() throws SemanticException {
        globalScope.addField(new Symbol("x", VarType.INT, false, false));
        Symbol resolved = childScope.resolve("x");
        assertNotNull(resolved);
        assertEquals(VarType.INT, resolved.getType());
    }

    @Test
    void duplicateFieldThrowsException() throws SemanticException {
        globalScope.addField(new Symbol("x", VarType.INT, false, false));
        assertThrows(SemanticException.class, new Executable() {
			@Override
			public void execute() throws Throwable {
				globalScope.addField(new Symbol("x", VarType.CSTR, false, false));
			}
		});
    }
}