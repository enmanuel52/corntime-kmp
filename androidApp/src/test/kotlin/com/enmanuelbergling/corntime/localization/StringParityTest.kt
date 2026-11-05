package com.enmanuelbergling.corntime.localization

import assertk.assertThat
import assertk.assertions.isEmpty
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test

/**
 * English and Spanish stay in step on both platforms: every translatable Android string and plural has a
 * Spanish (and, where a `values-en` exists, English) counterpart with the same placeholders, and every key
 * of the iOS String Catalog has a Spanish translation.
 */
class StringParityTest {

    private val root: File = generateSequence(File("").absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").exists() }

    private val ignoredDirs = setOf("build", ".git", ".gradle", ".claude", "node_modules")

    private data class Resource(val kind: String, val translatable: Boolean, val text: String)

    private fun resourceFiles(): List<File> = root.walkTopDown()
        .onEnter { it.name !in ignoredDirs }
        .filter { it.isFile && it.name == "strings.xml" && it.parentFile.name == "values" }
        .toList()

    private fun File.resources(): Map<String, Resource> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
        val result = linkedMapOf<String, Resource>()
        val nodes = document.documentElement.childNodes
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            val name = element.getAttribute("name")
            result[name] = Resource(
                kind = element.tagName,
                translatable = element.getAttribute("translatable") != "false",
                text = element.textContent.trim(),
            )
        }
        return result
    }

    private val placeholder = Regex("%(?:\\d+\\$)?[sdf]")

    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value.replace(Regex("^%\\d+\\$"), "%") }.sorted().toList()

    @Test
    fun givenAndroidModules_whenComparingSpanishToDefault_thenNothingIsMissingOrExtra() {
        val problems = mutableListOf<String>()
        resourceFiles().forEach { defaultFile ->
            val default = defaultFile.resources()
            val resDir = defaultFile.parentFile.parentFile
            val spanish = File(resDir, "values-es/strings.xml").takeIf { it.exists() }?.resources()
                ?: if (default.values.any { it.translatable }) {
                    problems += "${defaultFile.relativeTo(root)}: no values-es"
                    return@forEach
                } else return@forEach

            default.filter { it.value.translatable && it.key !in spanish }
                .forEach { problems += "${resDir.relativeTo(root)}: '${it.key}' missing in values-es" }
            spanish.filter { it.key !in default }
                .forEach { problems += "${resDir.relativeTo(root)}: '${it.key}' only in values-es" }
            spanish.filter { (name, es) -> default[name]?.let { it.kind == "string" && placeholders(it.text) != placeholders(es.text) } == true }
                .forEach { problems += "${resDir.relativeTo(root)}: '${it.key}' has different placeholders in values-es" }
        }
        assertThat(problems).isEmpty()
    }

    @Test
    fun givenAndroidModulesWithExplicitEnglish_whenComparingToDefault_thenTheyMirrorEachOther() {
        val problems = mutableListOf<String>()
        resourceFiles().forEach { defaultFile ->
            val resDir = defaultFile.parentFile.parentFile
            val english = File(resDir, "values-en/strings.xml").takeIf { it.exists() }?.resources() ?: return@forEach
            val default = defaultFile.resources()

            default.filter { it.value.translatable && it.key !in english }
                .forEach { problems += "${resDir.relativeTo(root)}: '${it.key}' missing in values-en" }
            english.filter { (name, en) -> default[name]?.text?.let { it != en.text } == true }
                .forEach { problems += "${resDir.relativeTo(root)}: '${it.key}' differs between values and values-en" }
        }
        assertThat(problems).isEmpty()
    }

    @Test
    fun givenIosStringCatalog_whenCheckingSpanish_thenEveryTranslatableKeyHasATranslation() {
        val catalog = File(root, "iosApp/iosApp/Resources/Localizable.xcstrings")
        val strings = Json.parseToJsonElement(catalog.readText()).jsonObject.getValue("strings").jsonObject

        val problems = strings.mapNotNull { (key, entry) ->
            val body = entry.jsonObject
            if (body["shouldTranslate"]?.jsonPrimitive?.boolean == false) return@mapNotNull null
            val spanish = body["localizations"]?.jsonObject?.get("es")?.jsonObject
            val translated = spanish != null && spanish.hasText()
            "'$key' has no Spanish translation".takeIf { !translated }
        }
        assertThat(problems).isEmpty()
    }

    /** A plain translation, or plural variations whose every form is filled in. */
    private fun JsonObject.hasText(): Boolean {
        val unit = get("stringUnit")?.jsonObject
        if (unit != null) return !unit["value"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()
        val plural = get("variations")?.jsonObject?.get("plural")?.jsonObject ?: return false
        return plural.isNotEmpty() && plural.values.all { it.jsonObject.hasText() }
    }
}
