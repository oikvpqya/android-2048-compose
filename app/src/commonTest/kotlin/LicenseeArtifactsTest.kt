import com.alexjlockwood.twentyfortyeight.domain.Artifact
import com.alexjlockwood.twentyfortyeight.domain.DEFAULT_JSON
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LicenseeArtifactsTest {

    @Test
    fun decodeArtifactsJson() = runTest {
        val artifacts = DEFAULT_JSON.decodeFromString<List<Artifact>>(artifactsJson)
        assertIs<List<Artifact>>(artifacts)
        assertTrue { artifacts.isNotEmpty() }
    }
}

private val artifactsJson = """
[
    {
        "groupId": "com.example",
        "artifactId": "example",
        "version": "1.0.0",
        "spdxLicenses": [
            {
                "identifier": "Apache-2.0",
                "name": "Apache License 2.0",
                "url": "https://www.apache.org/licenses/LICENSE-2.0"
            }
        ]
    }
]
""".trimIndent()
