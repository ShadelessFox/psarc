package sh.adelessfox.psarc.cli;

import picocli.CommandLine;

@CommandLine.Command(
    name = "psarc",
    mixinStandardHelpOptions = true,
    subcommands = {
        ExtractCommand.class,
        ListCommand.class
    }
)
public final class ApplicationCLI {
    public static void launch(String[] args) {
        new CommandLine(ApplicationCLI.class).execute(args);
    }
}
