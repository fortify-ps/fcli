package com.fortify.cli.ssc.picocli.command.appversion_attribute;

import picocli.CommandLine.Command;

@Command(
        name = "application-version-attribute",
        aliases = {"av-attribute"},
        subcommands = {
        	SSCApplicationVersionAttributeListCommand.class
        }
)
public class SSCApplicationVersionAttributeCommands {
}
