package com.fortify.cli.common.picocli.mixin.output;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.transform.flatten.FlattenTransformer;
import com.fortify.cli.common.json.transform.jsonpath.JsonPathTransformer;
import com.fortify.cli.common.output.IRecordWriter;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.output.RecordWriterConfig;
import com.fortify.cli.common.rest.unirest.exception.IfFailureHandler;

import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.StringUtils;
import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@ReflectiveAccess
public class OutputMixin {
	@Spec(Spec.Target.MIXEE) CommandSpec mixee;
	
	@ArgGroup(headingKey = "arggroup.output.heading", exclusive = false)
	private OutputOptionsArgGroup outputOptionsArgGroup;

	private static final class OutputOptionsArgGroup {
	    @CommandLine.Option(names = {"--fmt", "--format"}, order=1)
	    private OutputFormat outputFormat;
	
	    @CommandLine.Option(names = {"--fields"}, order=2)
		@Setter
	    private String fields;
	    
	    @CommandLine.Option(names = {"--flatten"}, order=3, defaultValue = "false")
	    @Getter
	    private boolean flatten;
	    
	    @CommandLine.Option(names = "--no-headers", negatable = true, order=3)
	    @Getter
	    private boolean withHeaders = true;
	
		@CommandLine.Option(names = {"--json-path"}, order = 6)
		@Getter private String jsonPath;
		
		@CommandLine.Option(names = {"-o", "--output"}, order=7)
	    private String outputFile; 
	}
	
	public OutputOptionsWriter getWriter() {
		return new OutputOptionsWriter(getOutputOptionsWriterConfig());
	}
	
	public void write(JsonNode jsonNode) {
		write(writer->writer::write, jsonNode);
	}

	public void write(HttpRequest<?> httpRequest) {
		write(writer->writer::write, httpRequest);
	}
	
	public void write(HttpRequest<?> httpRequest, Function<HttpResponse<JsonNode>, String> nextPageUrlProducer) {
		write(writer->writer::write, httpRequest, nextPageUrlProducer);
	}
	
	public void write(HttpResponse<JsonNode> httpResponse) {
		write(writer->writer::write, httpResponse);
	}

	public void overrideOutputFields(String fields) {
		if ( outputOptionsArgGroup == null){
			outputOptionsArgGroup = new OutputOptionsArgGroup();
		}
		outputOptionsArgGroup.setFields(fields);
	}
	
	private <T> void write(Function<OutputOptionsWriter, Consumer<T>> consumer, T input) {
		try ( var writer = getWriter() ) {
			consumer.apply(writer).accept(input);
		}
	}
	
	private <T1, T2> void write(Function<OutputOptionsWriter, BiConsumer<T1, T2>> consumer, T1 input1, T2 input2) {
		try ( var writer = getWriter() ) {
			consumer.apply(writer).accept(input1, input2);
		}
	}
	
	private OutputConfig getOutputOptionsWriterConfig() {
		Object mixeeObject = mixee.userObject();
		if ( mixeeObject instanceof IOutputConfigSupplier ) {
			return ((IOutputConfigSupplier)mixeeObject).getOutputOptionsWriterConfig();
		} else {
			return new OutputConfig();
		}
	}
	
	public final class OutputOptionsWriter implements AutoCloseable { // TODO Implement interface, make implementation private
		private final OutputMixin optionsHandler = OutputMixin.this;
		private final OutputOptionsArgGroup optionsArgGroup = optionsHandler.outputOptionsArgGroup!=null ? optionsHandler.outputOptionsArgGroup : new OutputOptionsArgGroup();
		private final OutputConfig config;
		private final OutputFormat outputFormat;
		private final PrintWriter printWriter;
		private final IRecordWriter recordWriter;
		
		public OutputOptionsWriter(OutputConfig config) {
			this.config = config;
			this.outputFormat = getOutputFormat();
			this.printWriter = createPrintWriter(config);
			this.recordWriter = outputFormat.getRecordWriterFactory().createRecordWriter(createOutputWriterConfig());
		}
		
		public void write(JsonNode jsonNode) {
			jsonNode = config.applyInputTransformations(outputFormat, jsonNode);
			if ( jsonNode!=null ) {
				if ( jsonNode.isArray() ) {
					jsonNode.elements().forEachRemaining(this::writeRecord);
				} else if ( jsonNode.isObject() ) {
					writeRecord(jsonNode);
				} else {
					throw new RuntimeException("Not sure what to do here");
				}
			}
		}
		
		public void write(HttpRequest<?> httpRequest) {
			httpRequest.asObject(JsonNode.class)
				.ifSuccess(this::write)
				.ifFailure(IfFailureHandler::handle); // Just in case no error interceptor was registered for this request
		}
		
		@SuppressWarnings("unchecked") // TODO Can we get rid of this warning in a better way?
		public void write(HttpRequest<?> httpRequest, Function<HttpResponse<JsonNode>, String> nextPageUrlProducer) {
			httpRequest.asPaged(r->r.asObject(JsonNode.class), nextPageUrlProducer)
				.ifSuccess(this::write)
				.ifFailure(IfFailureHandler::handle); // Just in case no error interceptor was registered for this request
		}

		public void write(HttpResponse<JsonNode> httpResponse) {
			write(httpResponse.getBody());
		}

		@SneakyThrows
		private void writeRecord(JsonNode jsonNode) {
			jsonNode = config.applyRecordTransformations(outputFormat, jsonNode); // TODO Before or after other transformations?
			jsonNode = applyJsonPathTransformation(outputFormat, jsonNode);
			jsonNode = config.applyFieldsTransformations(outputFormat, optionsArgGroup.fields, new I18nDefaultFieldNameFormatterProvider(), jsonNode);
			jsonNode = applyFlattenTransformation(outputFormat, jsonNode);
			if(jsonNode.getNodeType() == JsonNodeType.ARRAY) {
				if(jsonNode.size()>0) recordWriter.writeRecord((ObjectNode) new ObjectMapper().readTree(jsonNode.get(0).toString()));
			} else {
				recordWriter.writeRecord((ObjectNode) jsonNode);
			}
		}
		
		// TODO Move to OutputOptionsWriterConfig to allow defaults?
		protected JsonNode applyJsonPathTransformation(OutputFormat outputFormat, JsonNode data) {
			if ( StringUtils.isNotEmpty(optionsArgGroup.jsonPath) ) {
				data = new JsonPathTransformer(optionsArgGroup.jsonPath).transform(data);
			}
			return data;
		}
		
		// TODO Move to OutputOptionsWriterConfig to allow defaults?
		protected JsonNode applyFlattenTransformation(OutputFormat outputFormat, JsonNode data) {
			if ( optionsArgGroup.flatten ) {
				data = new FlattenTransformer(outputFormat.getDefaultFieldNameFormatter(), ".", false).transform(data);
			}
			return data;
		}

		private OutputFormat getOutputFormat() {
	    	OutputFormat result = optionsArgGroup.outputFormat;
	    	if ( result == null ) {
	    		result = config.defaultFormat();
	    	}
	    	if ( result == null ) {
	    		result = OutputFormat.table;
	    	}
	    	return result;
	    }
		
		private RecordWriterConfig createOutputWriterConfig() {
			return RecordWriterConfig.builder()
					.printWriter(printWriter)
					.headersEnabled(optionsArgGroup.isWithHeaders())
					.build();
		}
		
		private final PrintWriter createPrintWriter(OutputConfig config) {
			try {
				return optionsArgGroup.outputFile == null || "-".equals(optionsArgGroup.outputFile)
						? new PrintWriter(System.out)
						: new PrintWriter(optionsArgGroup.outputFile);
			} catch ( FileNotFoundException e) {
				throw new IllegalArgumentException("Output file "+optionsArgGroup.outputFile.toString()+" cannot be accessed");
			}
		}

		@Override
		public void close() {
			recordWriter.finishOutput();
			printWriter.flush();
			// TODO Close printwriter and/or underlying streams except for System.out
			//      once we have implemented output to file.
		}
	}
	
	private final class I18nDefaultFieldNameFormatterProvider implements IDefaultFieldNameFormatterProvider {
		private final CommandLine.Model.Messages messages;
		I18nDefaultFieldNameFormatterProvider() {
			ResourceBundle resourceBundle = mixee.resourceBundle();
			messages = resourceBundle==null ? null : new CommandLine.Model.Messages(mixee, resourceBundle);
		}
		@Override
		public Function<String, String> getDefaultFieldNameFormatter(OutputFormat outputFormat) {
			return field -> getDefaultFieldName(outputFormat, field);
		}

		private String getDefaultFieldName(OutputFormat outputFormat, String field) {
			String[] keys = {
				String.format("output.%s.field.%s.name", outputFormat.name(), field),
				String.format("output.field.%s.name", field),
			};
			
			return Stream.of(keys)
			.map(this::getMessageString)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(outputFormat.getDefaultFieldNameFormatter().apply(field));
		}
		
		private String getMessageString(String key) {
			return messages==null ? null : messages.getString(key, null);
		}
	}
}


