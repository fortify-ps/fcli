package com.fortify.cli;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

import com.fortify.cli.common.command.FCLIRootCommand;
import com.fortify.cli.common.command.util.DefaultValueProvider;
import com.fortify.cli.common.command.util.SubcommandOf;
import com.oracle.svm.core.annotate.AutomaticFeature;

import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.qualifiers.Qualifiers;
import picocli.CommandLine;

public class FortifyCLI {
	private static int execute(Class<?> clazz, String[] args) {
		try (ApplicationContext context = ApplicationContext.builder(FortifyCLI.class, Environment.CLI).start()) {
			FCLIRootCommand rootCommand = context.getBean(FCLIRootCommand.class);
			MicronautFactory factory = new MicronautFactory(context);
			CommandLine commandLine = new CommandLine(rootCommand, factory)
					.setCaseInsensitiveEnumValuesAllowed(true)
					.setDefaultValueProvider(new DefaultValueProvider())
					.setUsageHelpAutoWidth(true);
			addSubcommands(context, factory, commandLine, rootCommand);
			return commandLine.execute(args);
		}
	}

	private static void addSubcommands(ApplicationContext context, MicronautFactory factory, CommandLine commandLine,
			FCLIRootCommand rootCommand) {
		Map<Class<?>, List<Object>> parentToSubcommandsMap = getParentToSubcommandsMap(context);
		addSubcommands(parentToSubcommandsMap, factory, commandLine, rootCommand);
	}

	private static final void addSubcommands(Map<Class<?>, List<Object>> parentToSubcommandsMap,
			MicronautFactory factory, CommandLine commandLine, Object command) {
		List<Object> subcommands = parentToSubcommandsMap.get(command.getClass());
		if (subcommands != null) {
			for (Object subcommand : subcommands) {
				CommandLine subCommandLine = new CommandLine(subcommand, factory);
				try {
					commandLine.addSubcommand(subCommandLine);
				} catch ( RuntimeException e ) {
					throw new RuntimeException("Error while adding command class "+subcommand.getClass().getName(), e);
				}
				addSubcommands(parentToSubcommandsMap, factory, subCommandLine, subcommand);
			}
		}
	}

	private static final Map<Class<?>, List<Object>> getParentToSubcommandsMap(ApplicationContext context) {
		final var beanDefinitions = context.getBeanDefinitions(Qualifiers.byStereotype(SubcommandOf.class));
		
		/* Disabled for now as for some reason compilation intermittently fails on this statement
		return beanDefinitions.stream().collect(
			Collectors.groupingBy(bd -> bd.getAnnotation(SubcommandOf.class).classValue().get(),
			                      Collectors.mapping(context::getBean, Collectors.toList())));
		*/
		var parentToSubcommandsMap = new LinkedHashMap<Class<?>, List<Object>>();
		beanDefinitions.stream()
			// TODO Filter by enabled-products 
			.sorted(FortifyCLI::compare)
			.forEach(bd ->
				addMultiValueEntry(
						parentToSubcommandsMap, 
						getParentCommandClazz(bd),
						context.getBean(bd)));
		// TODO Remove commands that do not have any runnable or callable children (because those have been filtered based on product 
		return parentToSubcommandsMap;
	}

	private static final Class<?> getParentCommandClazz(BeanDefinition<?> bd) {
		Optional<Class<?>> optClazz = bd.getAnnotation(SubcommandOf.class).classValue();
		if ( !optClazz.isPresent() ) {
			throw new IllegalStateException("No parent command found for class "+bd.getBeanType().getName());
		}
		return optClazz.get();
	}
	
	private static int compare(BeanDefinition<?> bd1, BeanDefinition<?> bd2) {
		return Integer.compare(OrderUtil.getOrder(bd1.getAnnotationMetadata()), OrderUtil.getOrder(bd2.getAnnotationMetadata()));
	}

	private static <K, V> void addMultiValueEntry(LinkedHashMap<K, List<V>> map, K key, V value) {
		map.computeIfAbsent(key, k->new LinkedList<V>()).add(value);
	}

	public static void main(String[] args) {
		int exitCode = execute(FCLIRootCommand.class, args);
		System.exit(exitCode);
	}
	
	@AutomaticFeature
	public static final class RuntimeReflectionRegistrationFeature implements Feature {
		public void beforeAnalysis(BeforeAnalysisAccess access) {
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
