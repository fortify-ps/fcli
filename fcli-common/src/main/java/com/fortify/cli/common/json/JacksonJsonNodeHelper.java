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
package com.fortify.cli.common.json;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

/**
 * This bean provides utility methods for working with Jackson JsonNode trees.
 * 
 * @author Ruud Senden
 *
 */
public class JacksonJsonNodeHelper {
	private static final ObjectMapper objectMapper = _createObjectMapper();
	private final ParseContext parseContext;
	private static final JacksonJsonNodeHelper INSTANCE = new JacksonJsonNodeHelper();

	public JacksonJsonNodeHelper() {
		this.parseContext = JsonPath.using(Configuration.builder()
				.jsonProvider(new JacksonJsonNodeJsonProvider(objectMapper))
				.mappingProvider(new JacksonMappingProvider(objectMapper))
				.options(EnumSet.noneOf(Option.class))
				.build());
	}
	
	private static final ObjectMapper _createObjectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        objectMapper.configure(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS, true);
        return objectMapper;
	}

	public static final <R> R evaluateJsonPath(Object input, String path, Class<R> returnClass) {
		return INSTANCE.parseContext.parse(input).read(path, returnClass);
	}
	
	public static final ObjectNode getFirstObjectNode(JsonNode input) {
		if ( input instanceof ObjectNode ) {
			return (ObjectNode)input;
		} else if ( input instanceof ArrayNode ) {
			ArrayNode array = (ArrayNode)input;
			if ( array.size()==0 ) { return null; }
			JsonNode node = array.get(0);
			if ( node instanceof ObjectNode ) {
				return (ObjectNode)node;
			}
		}
		throw new IllegalArgumentException("Input must be an ObjectNode or array of ObjectNodes");
	}

	public static JsonNode filterJsonNode (JsonNode node, Set<String> outputFields){
		Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
		while (nodeFields.hasNext()) {
			Map.Entry<String, JsonNode> nodeField = nodeFields.next();
			if( !outputFields.contains(nodeField.getKey())) { nodeFields.remove(); }
		}

		return node;
	}
}
