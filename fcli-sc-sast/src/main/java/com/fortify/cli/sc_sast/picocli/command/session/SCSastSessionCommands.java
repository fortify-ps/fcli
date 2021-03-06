package com.fortify.cli.sc_sast.picocli.command.session;

import picocli.CommandLine.Command;

@Command(
        name = "session",
        subcommands = {
        		SCSastSessionListCommand.class,
                SCSastSessionLoginCommand.class,
                SCSastSessionLogoutCommand.class 
        }
)
public class SCSastSessionCommands {
}
