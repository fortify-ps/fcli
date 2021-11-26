/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.sc_dast.picocli.command.dast_scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.sc_dast.picocli.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.picocli.command.util.SCDastScanActionsHandler;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@SubcommandOf(SCDastScanCommand.class)
@Command(name = "resume", description = "Resumes a DAST scan on ScanCentral DAST")
@Order(SCDastScanCommandsOrder.RESUME)
public final class SCDastScanResumeCommand extends AbstractSCDastUnirestRunnerCommand {
    @Spec CommandSpec spec;
    @ArgGroup(exclusive = false, heading = "Resume scan options:%n", order = 1)
    @Getter private SCDastScanResumeOptions resumeScanOptions;

    @Mixin private OutputMixin outputMixin;
    
    @ReflectiveAccess
    public static class SCDastScanResumeOptions {
        @Option(names = {"-i","--id", "--scan-id"}, description = "The scan id.", required = true)
        @Getter private int scanId;

        @Option(names = {"-w", "--wait", "--wait-resumed"}, defaultValue = "false",
                description = "Wait until the scan is running")
        @Getter private boolean waitResumed;

        @Option(names = {"--interval", "--wait-interval"}, defaultValue = "30",
                description = "When waiting for completion, how long between to poll, in seconds", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
        @Getter private int waitInterval;
    }

    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        if(resumeScanOptions == null){
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "Error: No parameter found. Provide the required scan-settings identifier.");
        }
        SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
        JsonNode response = actionsHandler.resumeScan(resumeScanOptions.getScanId());

        if(response != null) outputMixin.write(response);

        if(resumeScanOptions.isWaitResumed()){ actionsHandler.waitResumed(resumeScanOptions.getScanId(), resumeScanOptions.getWaitInterval()); }

        return null;
    }
}