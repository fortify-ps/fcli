package com.fortify.cli.fod.command.entity.session;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(
        name = "session",
        description = "Commands to manage Fortify SSC sessions.",
        subcommands = {
                FODSessionLoginCommand.class,
                FODSessionLogoutCommand.class
        }
)
public class FODSessionCommands {
}
