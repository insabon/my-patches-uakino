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
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

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

        // Подія не відправляється, оверлей не зʼявляється.
        method.replaceInstruction(dispatchIdx, "nop")
    }
}

// uk.jt8: корутина, що резолвить прероллу (uk.wf9). Метод уже повертає null,
// коли реклами немає (ss8 == null, "Collection is empty."), тож null безпечний.
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

        // check-cast Luk/wf9; одразу перед return-object
        val targets = insns.indices.filter { i ->
            val insn = insns[i]
            insn.opcode == Opcode.CHECK_CAST &&
                ((insn as ReferenceInstruction).reference as? TypeReference)?.type == "Luk/wf9;" &&
                insns.getOrNull(i + 1)?.opcode == Opcode.RETURN_OBJECT
        }
        check(targets.isNotEmpty()) { "return (wf9) not found in jt8" }

        // з кінця, щоб індекси не збивались
        targets.sortedDescending().forEach { i ->
            val reg = (insns[i + 1] as OneRegisterInstruction).registerA
            method.addInstructions(i + 1, "const/16 v$reg, 0x0")
        }
    }
}

// uk.r62: відповідь сервера про оновлення (H = uu: A = minSupportedVC, B = latestVC).
// a(long): minSupportedVC > поточна версія -> обовʼязкове оновлення.
// b(long): поточна версія менша за останню -> є оновлення.
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

// ---- UAKino 1.14: ті самі перевірки, але інші імена (r62 -> ts1) ----

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

// ---- Налагодження: довіра до користувацьких сертифікатів (для перехоплення трафіку mitmproxy) ----
// У network_security_config.xml дозволено лише системні сертифікати. Додаємо src="user".
// УВАГА: знижує безпеку застосунку. Вмикати лише для налагодження (default = false).

private val UAKINO_DEBUG_COMPATIBILITY = Compatibility(
    name = "UAKino",
    packageName = "ua.kino",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xE50914,
    targets = listOf(
        AppTarget(version = "1.13"),
        AppTarget(version = "1.14"),
        AppTarget(version = "1.15")
    )
)

@Suppress("unused")
val trustUserCertificatesPatch = resourcePatch(
    name = "Trust user certificates (debug, 1.13-1.15)",
    description = "UAKino 1.13, 1.14, 1.15. For traffic inspection only: allows user-installed CA certificates.",
    default = false
) {
    compatibleWith(UAKINO_DEBUG_COMPATIBILITY)

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
