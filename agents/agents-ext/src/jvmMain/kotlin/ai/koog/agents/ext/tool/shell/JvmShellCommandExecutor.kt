package ai.koog.agents.ext.tool.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shell command executor using ProcessBuilder for JVM platforms.
 *
 * @see ShellCommandExecutor
 */
public class JvmShellCommandExecutor : ShellCommandExecutor() {

    private val outBuf = StringBuilder()
    private val errBuf = StringBuilder()

    private companion object {
        val IS_WINDOWS = System.getProperty("os.name")
            .lowercase()
            .contains("win")
    }

    /**
     * Executes a shell command and returns combined output and exit code.
     *
     * @param command Shell command string to execute
     * @param workingDirectory Working directory, or null to use current directory
     * @return process exit code
     */
    public override suspend fun execute(
        command: String,
        workingDirectory: String?
    ): Int = withContext(Dispatchers.IO) {
        // reset buffers
        outBuf.setLength(0)
        errBuf.setLength(0)

        val process = ProcessBuilder(
            if (IS_WINDOWS) {
                listOf("cmd.exe", "/c", command)
            } else {
                listOf("sh", "-c", command)
            }
        ).apply {
            workingDirectory?.let { directory(File(it)) }
        }.start()

        async(Dispatchers.IO) {
            process.inputStream.reader().use { outBuf.append(it.readText()) }
        }
        async(Dispatchers.IO) {
            process.errorStream.reader().use { errBuf.append(it.readText()) }
        }

        val exitCode = process.waitFor()
        exitCode
    }

    /**
     * Collect the logs of the last command having been executed.
     */
    public override suspend fun collectLogs() : String {
        return listOf(outBuf.toString(), errBuf.toString())
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}
