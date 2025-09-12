package com.aidestinymaster.features.ziwei

fun ZiweiSummary.toMixedSnippet(): String {
    val sb = StringBuilder()
    sb.appendLine("[Ziwei]")
    sb.appendLine("title: ${this.title}")
    if (highlights.isNotEmpty()) {
        sb.appendLine("highlights:")
        highlights.forEach { h -> sb.appendLine("- $h") }
    }
    return sb.toString()
}
