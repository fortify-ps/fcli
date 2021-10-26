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
package com.fortify.cli.common.output;

import com.fortify.cli.common.output.writer.CsvOutputWriterFactory;
import com.fortify.cli.common.output.writer.IOutputWriterFactory;
import com.fortify.cli.common.output.writer.JsonOutputWriterFactory;
import com.fortify.cli.common.output.writer.TableOutputWriterFactory;
import com.fortify.cli.common.output.writer.TreeOutputWriterFactory;
import com.fortify.cli.common.output.writer.XmlOutputWriterFactory;
import com.fortify.cli.common.output.writer.YamlOutputWriterFactory;

import lombok.Getter;

public enum OutputFormat {
	json(new JsonOutputWriterFactory()), 
	yaml(new YamlOutputWriterFactory()), 
	table(new TableOutputWriterFactory()), 
	tree(new TreeOutputWriterFactory()), 
	xml(new XmlOutputWriterFactory()), 
	csv(new CsvOutputWriterFactory());
	
	@Getter private final IOutputWriterFactory outputWriterFactory;
	private OutputFormat(IOutputWriterFactory outputWriterFactory) {
		this.outputWriterFactory = outputWriterFactory;
	}
}