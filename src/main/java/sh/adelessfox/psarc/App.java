package sh.adelessfox.psarc;

import javafx.application.Application;
import sh.adelessfox.psarc.cli.ApplicationCLI;

public class App {
    public static void main(String[] args) {
        if (args.length > 0) {
            ApplicationCLI.launch(args);
        } else {
            Application.launch(AppWindow.class, args);
        }
    }
}