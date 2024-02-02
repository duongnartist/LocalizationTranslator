import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.file.Files
import kotlin.io.path.Path


object LocalizationTranslator {
    fun translate(source: String, target: String, text: String): String {
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&sl=$source&tl=$target&q=$text"
        val request = Request.Builder().url(url).build()
        OkHttpClient().newCall(request).execute().use { response ->
            val output = response.body?.string() ?: ""
            return output.split(",").first().replace("[", "")
                .replace("\"", "").replace("\\u200b", "")
        }
    }
}

fun main() {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val baseLanguage = "en"
    val targetLanguages = listOf("vi","zh", "ja", "ko")
    val baseJson = Files.readString(Path("l10n/intl_$baseLanguage.arb"))
    val typeToken = object: TypeToken<Map<String, Any>>(){}.type
    val baseTree = gson.fromJson<Map<String, Any>>(baseJson, typeToken)

    targetLanguages.forEach { targetLanguage ->
        println("Translate to $targetLanguage")
        val targetTree = mutableMapOf<String, Any>()
        println("Translating! From $baseLanguage to $targetLanguage!")
        baseTree.forEach { (key, value) ->
            if (key.contains("@")) {
                targetTree[key] = value
            } else {
                val output = LocalizationTranslator.translate(baseLanguage, targetLanguage, value as String)
                println("  Translated $key from $value to $output!")
                targetTree[key] = output
            }
        }
        val targetJson = gson.toJson(targetTree)
        Files.writeString(Path("l10n/intl_$targetLanguage.arb"), targetJson)
        println("Translated! From $baseLanguage to $targetLanguage!")
    }

}