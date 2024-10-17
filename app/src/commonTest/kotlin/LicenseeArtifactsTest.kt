import com.alexjlockwood.twentyfortyeight.Res
import com.alexjlockwood.twentyfortyeight.domain.Artifact
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LicenseeArtifactsTest {

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun loadResource() = runTest {
        val artifacts = Json.decodeFromString<List<Artifact>>(Res.readBytes("files/artifacts.json").decodeToString())
        assertIs<List<Artifact>>(artifacts)
        assertTrue { artifacts.isNotEmpty() }
    }
}
