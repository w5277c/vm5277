/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.lsp.nb.lsp.plugin.asm;

import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;


@ActionID(category = "Build", id = "ru.vm5277.lsp.nb.lsp.plugin.asm.CompileASMPopupAction")
@ActionRegistration(displayName = "#CTL_CompileASMAction", lazy = false)
@ActionReference(path = "Editors/text/x-vm5277-asm/Popup", position = 100)
@Messages("CTL_CompileASMAction=Compile ASM")

public class ASMCompilePopupAction extends ASMCompileAction {

	public ASMCompilePopupAction() {
		super();
	}
}
