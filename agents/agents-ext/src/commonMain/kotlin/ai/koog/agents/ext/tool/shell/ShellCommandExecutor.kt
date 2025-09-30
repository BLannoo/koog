package ai.koog.agents.ext.tool.shell

/**
 * Shell command executor using platform-specific shells (cmd.exe on Windows, sh on Unix).
 */
public abstract class ShellCommandExecutor {
    /**
     * Executes a command and captures what it prints.
     *
     * @param command Command string (e.g., "ls -la | grep txt")
     * @param workingDirectory Working directory, or null to use the current directory
     * @return exit code
     */
    public abstract suspend fun execute(command: String, workingDirectory: String?): Int

    /**
     * Collect the logs of the last command having been executed.
     */
    public abstract suspend fun collectLogs(): String
}
