/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.config.FcliConfig;
import com.fortify.cli.common.locale.LanguageHelper;
import io.micronaut.configuration.picocli.PicocliRunner;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.jasypt.normalization.Normalizer;

import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.context.ApplicationContext;

/**
 * <p>This class provides the {@link #main(String[])} entrypoint into the application. 
 * It first configures logging and then loads the {@link PicocliRunner} class to
 * actually execute commands based on provided command line arguments.</p>
 * 
 * <p>This class is also responsible for registering some GraalVM features, allowing
 * the application to run properly as GraalVM native images.</p>
 * 
 * @author Ruud Senden
 */
public class FortifyCLI {
	// TODO: I'm not too sure that I feel happy with this. It just feels wrong.
	private static final LanguageHelper languageHelper = new LanguageHelper(new FcliConfig(new ObjectMapper()));

	/**
	 * This is the main entry point for executing the Fortify CLI. It will configure logging and
	 * then get a {@link PicocliRunner} instance from Micronaut, which will perform the
	 * actual work in its {@link PicocliRunner#execute(Class, String...)} method.
	 * @param args Command line options passed to Fortify CLI
	 */
	public static void main(String[] args) {
		//Locale.setDefault(new Locale("nl"));
		//new CommandLine(new InitLocale()).parseArgs(args);
		FortifyCLILogHelper.configureLogging(args);
		languageHelper.configureLanguage();
		System.exit(execute(args));
	}

	/**
	 * This method starts the Micronaut {@link ApplicationContext}, then invokes the 
	 * {@link PicocliRunner#execute(Class, String...)} method on the {@link PicocliRunner}
	 * singleton retrieved from the Micronaut {@link ApplicationContext}
	 * @param args Command line options passed to Fortify CLI
	 * @return exit code
	 */
	private static int execute(String[] args) {
		return PicocliRunner.execute(FCLIRootCommands.class, args);
	}
	
	/**
	 * Register classes for runtime reflection in GraalVM native images
	 */
	@AutomaticFeature
	public static final class RuntimeReflectionRegistrationFeature implements Feature {
		public void beforeAnalysis(BeforeAnalysisAccess access) {
			// This jasypt class uses reflection, so we perform a dummy operation to have GraalVM native image generation detect this
			Normalizer.normalizeToNfc("dummy");
			
			// TODO Review whether these are all necessary
			RuntimeReflection.register(String.class);
			RuntimeReflection.register(LogFactoryImpl.class);
			RuntimeReflection.register(LogFactoryImpl.class.getDeclaredConstructors());
			RuntimeReflection.register(LogFactory.class);
			RuntimeReflection.register(LogFactory.class.getDeclaredConstructors());
			RuntimeReflection.register(SimpleLog.class);
			RuntimeReflection.register(SimpleLog.class.getDeclaredConstructors());
		}
	}
}
