package app.template.patches.example

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// UAKino 1.15 (ua.kino). Імена класів обфусковані й діють лише для цієї версії.
// uk.kd9 -> case 16: qf4.c(new em8(url)) відправляє подію ShowAdQr.
object ShowAdQrFingerprint : Fingerprint(
    definingClass = "Luk/kd9;",
    filters = listOf(
        methodCall(definingClass = "Luk/em8;", name = "<init>"),
        methodCall(definingClass = "Luk/qf4;", name = "c"),
    )
)

private fun isCall(insn: Instruction, cls: String, name: String): Boolean {
    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: return false
    return ref.definingClass == cls && ref.name == name
}

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Disables the ShowAdQr ad overlay.",
    default = true
) {
    execute {
        val method = ShowAdQrFingerprint.method
        val insns = method.implementation!!.instructions.toList()

        val initIdx = insns.indexOfFirst { isCall(it, "Luk/em8;", "<init>") }
        check(initIdx != -1) { "em8 constructor call not found" }

        val dispatchIdx = (initIdx + 1 until insns.size).first {
            isCall(insns[it], "Luk/qf4;", "c")
        }

        // Подія не відправляється, оверлей не зʼявляється.
        method.replaceInstruction(dispatchIdx, "nop")
    }
}
