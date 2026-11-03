package app.template.patches.example

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

// ============================================================================
// UAKino 1.15 (ua.kino)
// ============================================================================

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

private val UAKINO_COMPATIBILITY = Compatibility(
    name = "UAKino",
    packageName = "ua.kino",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xE50914,
    targets = listOf(
        AppTarget(version = "1.15")
    )
)

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads (1.15)",
    description = "UAKino 1.15 only. Disables the ShowAdQr ad overlay.",
    default = true
) {
    compatibleWith(UAKINO_COMPATIBILITY)

    execute {
        val method = ShowAdQrFingerprint.method
        val insns = method.implementation!!.instructions.toList()

        val initIdx = insns.indexOfFirst { isCall(it, "Luk/em8;", "<init>") }
        check(initIdx != -1) { "em8 constructor call not found" }

        val dispatchIdx = (initIdx + 1 until insns.size).first {
            isCall(insns[it], "Luk/qf4;", "c")
        }

        method.replaceInstruction(dispatchIdx, "nop")
    }
}

object PrerollResolverFingerprint : Fingerprint(
    definingClass = "Luk/jt8;",
    filters = listOf(string("random"))
)

@Suppress("unused")
val removePrerollPatch = bytecodePatch(
    name = "Remove preroll ads (1.15)",
    description = "UAKino 1.15 only. Makes the preroll resolver return no ad, so the movie starts immediately.",
    default = true
) {
    compatibleWith(UAKINO_COMPATIBILITY)

    execute {
        val method = PrerollResolverFingerprint.method
        val insns = method.implementation!!.instructions.toList()

        val targets = insns.indices.filter { i ->
            val insn = insns[i]
            insn.opcode == Opcode.CHECK_CAST &&
                ((insn as ReferenceInstruction).reference as? TypeReference)?.type == "Luk/wf9;" &&
                insns.getOrNull(i + 1)?.opcode == Opcode.RETURN_OBJECT
        }
        check(targets.isNotEmpty()) { "return (wf9) not found in jt8" }

        targets.sortedDescending().forEach { i ->
            val reg = (insns[i + 1] as OneRegisterInstruction).registerA
            method.addInstructions(i + 1, "const/16 v$reg, 0x0")
        }
    }
}

object UpdateRequiredFingerprint : Fingerprint(
    definingClass = "Luk/r62;",
    name = "a",
    returnType = "Z",
    parameters = listOf("J"),
)

object UpdateAvailableFingerprint : Fingerprint(
    definingClass = "Luk/r62;",
    name = "b",
    returnType = "Z",
    parameters = listOf("J"),
)

@Suppress("unused")
val disableUpdatePromptPatch = bytecodePatch(
    name = "Disable update prompt (1.15)",
    description = "UAKino 1.15 only. Hides the update dialog and the forced-update check.",
    default = true
) {
    compatibleWith(UAKINO_COMPATIBILITY)

    execute {
        listOf(UpdateRequiredFingerprint, UpdateAvailableFingerprint).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """
            )
        }
    }
}

// ============================================================================
// UAKino 1.14
// ============================================================================

private val UAKINO_114_COMPATIBILITY = Compatibility(
    name = "UAKino",
    packageName = "ua.kino",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xE50914,
    targets = listOf(
        AppTarget(version = "1.14")
    )
)

object UpdateRequired114Fingerprint : Fingerprint(
    definingClass = "Luk/ts1;",
    name = "a",
    returnType = "Z",
    parameters = listOf("J"),
)

object UpdateAvailable114Fingerprint : Fingerprint(
    definingClass = "Luk/ts1;",
    name = "b",
    returnType = "Z",
    parameters = listOf("J"),
)

@Suppress("unused")
val disableUpdatePrompt114Patch = bytecodePatch(
    name = "Disable update prompt (1.14)",
    description = "UAKino 1.14 only. Hides the update dialog and the forced-update check.",
    default = true
) {
    compatibleWith(UAKINO_114_COMPATIBILITY)

    execute {
        listOf(UpdateRequired114Fingerprint, UpdateAvailable114Fingerprint).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """
            )
        }
    }
}

// ============================================================================
// UAKino 1.13
// ============================================================================

private val UAKINO_113_COMPATIBILITY = Compatibility(
    name = "UAKino",
    packageName = "ua.kino",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xE50914,
    targets = listOf(
        AppTarget(version = "1.13")
    )
)

// 1. Примусове ввімкнення DoH замість скидання на системний DNS (UnknownHostException)
object ForceDoh113Fingerprint : Fingerprint(
    definingClass = "Luk/kq2;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Luk/d92;", "Luk/wx1;"),
)

@Suppress("unused")
val forceDoh113Patch = bytecodePatch(
    name = "Force DoH (1.13)",
    description = "UAKino 1.13 only. Forces DNS-over-HTTPS on all channels, preventing fallback to ISP-blocked DNS.",
    default = true
) {
    compatibleWith(UAKINO_113_COMPATIBILITY)

    execute {
        ForceDoh113Fingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                return-object v0
            """
        )
    }
}

// 2. Дозвіл на встановлення користувацьких сертифікатів CA (для роботи з проксі)
@Suppress("unused")
val trustUserCertificates113Patch = resourcePatch(
    name = "Trust user certificates (1.13)",
    description = "UAKino 1.13 only. Allows user-installed CA certificates (for traffic inspection).",
    default = true
) {
    compatibleWith(UAKINO_113_COMPATIBILITY)

    execute {
        document("res/xml/network_security_config.xml").use { document ->
            val anchors = document.getElementsByTagName("trust-anchors")
            for (i in 0 until anchors.length) {
                val anchor = anchors.item(i) as Element
                val certificates = document.createElement("certificates")
                certificates.setAttribute("src", "user")
                anchor.appendChild(certificates)
            }
        }
    }
}
