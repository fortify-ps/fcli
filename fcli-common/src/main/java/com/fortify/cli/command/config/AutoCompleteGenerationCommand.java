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
package com.fortify.cli.command.config;

import com.fortify.cli.command.util.SubcommandOf;

import io.micronaut.core.annotation.ReflectiveAccess;
import picocli.AutoComplete;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;

@ReflectiveAccess
@SubcommandOf(RootConfigCommand.class)
@Command(name = "generate-completion", description = {
		"Generate bash/zsh completion script for ${ROOT-COMMAND-NAME:-the root command of this command}.",
		"Run the following command to give `${ROOT-COMMAND-NAME:-$PARENTCOMMAND}` TAB completion in the current shell:",
		"", 
		"  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})", 
		"" })
public final class AutoCompleteGenerationCommand implements Runnable {
	@Spec CommandLine.Model.CommandSpec spec;

	public void run() {
		String script = AutoComplete.bash(spec.root().name(), spec.root().commandLine());
		// not PrintWriter.println: scripts with Windows line separators fail in strange
		// ways!
		spec.commandLine().getOut().print(script);
		spec.commandLine().getOut().print('\n');
		spec.commandLine().getOut().flush();
	}
}
