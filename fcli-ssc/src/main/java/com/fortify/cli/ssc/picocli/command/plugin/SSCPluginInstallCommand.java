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
package com.fortify.cli.ssc.picocli.command.plugin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.domain.plugin.parser.xml.Plugin;
import com.fortify.cli.ssc.domain.uploadResponse.UploadResponse;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.rest.unirest.runner.SSCUnirestFileTransferRunner;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import com.jayway.jsonpath.JsonPath;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

@ReflectiveAccess
@Command(name = "install")
public class SSCPluginInstallCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Mixin private OutputMixin outputMixin;

	@CommandLine.Option(names = {"-f", "--file"}, required = true)
	private File pluginJarFile;

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		XmlMapper mapper = new XmlMapper();
		Plugin pluginXmlObj = mapper.readValue(getPluginXml(pluginJarFile.toPath()), Plugin.class);

		UploadResponse uploadResponse = SSCUnirestFileTransferRunner.Upload(
				unirest,
				SSCUrls.UPLOAD_PLUGIN,
				pluginJarFile.getPath().toString()
		);

		if(!uploadResponse.msg.value.toLowerCase().contains("success"))
			throw new RuntimeException(String.format("Plugin upload not successful:\n\tCODE: %s\n\tMSG: %s", uploadResponse.code.value, uploadResponse.msg.value.replace("\n"," ")));

		String id = "-1";
		int timesChecked=0;
		String searchQuery = String.format("$.data[?(@.pluginId == \"%s\")]", pluginXmlObj.id);
		while(true) {
			long millis = System.currentTimeMillis();
			HttpResponse response = unirest.get(SSCUrls.PLUGINS)
					.queryString("orderBy","-id")
					.queryString("limit","-1").asObject(ObjectNode.class);

			String candidate = JsonPath.parse(response.getBody().toString()).read(searchQuery).toString();

			if(candidate != null && !candidate.isBlank())
				id = JsonPath.parse(candidate).read("$.[0].id").toString();

			if(timesChecked > 5 || !id.equals("-1"))
				break;

			timesChecked += 1;
			Thread.sleep(1000 - millis % 1000);
		}

		if(id.equals("-1")){
			System.out.println("The plugin uploaded successfully, but fcli timed-out when trying to verify that the plugin has installed correctly. Please login to SSC to verify that the plugin has installed successfully.");
			System.exit(1);
		}

		outputMixin.write(
				unirest.get(SSCUrls.PLUGINS)
						.queryString("q", String.format("id:%d", Integer.parseInt(id)))
						.queryString("limit","1")
		);
		return null;
	}

	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
				.defaultColumns("id#pluginId#pluginType#pluginName#pluginVersion#pluginState");
	}

	public String getPluginXml(Path zipFile) throws IOException {
		try (FileSystem fileSystem = FileSystems.newFileSystem(zipFile, null)) {
			Path fileToExtract = fileSystem.getPath("plugin.xml");
			String pluginXml = Files.readString(fileToExtract);
			return pluginXml;
		}
	}
}
