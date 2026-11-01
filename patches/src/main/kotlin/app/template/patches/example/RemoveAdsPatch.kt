package app.template.patches.example

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
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

// ---- UAKino 1.13: експеримент з підміною версії для Firebase Remote Config ----
// ConfigFetchHttpClient.a(...) надсилає Google "appVersion" (versionName) і "appBuild" (versionCode).
// Підставляємо значення версії 1.15 (versionCode 28), щоб налаштування приходили як для 1.15.

private val UAKINO_113_COMPATIBILITY = Compatibility(
    name = "UAKino",
    packageName = "ua.kino",
    apkFileType = ApkFileType.APK,
    appIconColor = 0xE50914,
    targets = listOf(
        AppTarget(version = "1.13")
    )
)

object RemoteConfigRequestFingerprint : Fingerprint(
    definingClass = "/ConfigFetchHttpClient;",
    filters = listOf(string("appVersion"))
)

@Suppress("unused")
val spoofRemoteConfigVersion113Patch = bytecodePatch(
    name = "Spoof version as 1.15 for Remote Config (1.13)",
    description = "UAKino 1.13 only. Experimental: reports version 1.15 to Firebase Remote Config.",
    default = true
) {
    compatibleWith(UAKINO_113_COMPATIBILITY)

    execute {
        val method = RemoteConfigRequestFingerprint.method
        val insns = method.implementation!!.instructions.toList()

        // iget-object vX, vY, PackageInfo->versionName  ->  const-string vX, "1.15"
        val nameIdx = insns.indexOfFirst {
            val ref = (it as? ReferenceInstruction)?.reference as? FieldReference
            it.opcode == Opcode.IGET_OBJECT &&
                ref?.definingClass == "Landroid/content/pm/PackageInfo;" &&
                ref.name == "versionName"
        }
        check(nameIdx != -1) { "PackageInfo.versionName read not found" }
        val nameReg = (insns[nameIdx] as TwoRegisterInstruction).registerA
        method.replaceInstruction(nameIdx, "const-string v$nameReg, \"1.15\"")

        // invoke-static {PackageInfo}, X->C(PackageInfo)J ; move-result-wide vA  ->  const-wide/16 vA, 28
        val buildIdx = insns.indexOfFirst {
            val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
            it.opcode == Opcode.INVOKE_STATIC &&
                ref != null &&
                ref.returnType == "J" &&
                ref.parameterTypes.map { type -> type.toString() } ==
                listOf("Landroid/content/pm/PackageInfo;")
        }
        check(buildIdx != -1) { "versionCode helper call not found" }
        val resultInsn = insns[buildIdx + 1]
        check(resultInsn.opcode == Opcode.MOVE_RESULT_WIDE) { "move-result-wide not found" }
        val buildReg = (resultInsn as OneRegisterInstruction).registerA
        method.replaceInstruction(buildIdx + 1, "const-wide/16 v$buildReg, 0x1c")
    }
}
