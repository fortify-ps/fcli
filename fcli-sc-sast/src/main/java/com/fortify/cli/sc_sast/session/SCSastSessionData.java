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
package com.fortify.cli.sc_sast.session;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fortify.cli.common.session.AbstractSessionData;
import com.fortify.cli.common.session.summary.SessionSummary;
import com.fortify.cli.sc_sast.session.login.SCSastSessionLoginConfig;
import com.fortify.cli.sc_sast.util.SCSastConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data @EqualsAndHashCode(callSuper = true)  @ReflectiveAccess @JsonIgnoreProperties(ignoreUnknown = true)
public class SCSastSessionData extends AbstractSessionData {
	@Getter private char[] clientAuthToken;
	
	public SCSastSessionData() {}
	
	public SCSastSessionData(SCSastSessionLoginConfig config) {
		super(config.getConnectionConfig());
		this.clientAuthToken = config.getClientAuthToken();
	}
	
	@JsonIgnore @Override
	public String getSessionType() {
		return SCSastConstants.SESSION_TYPE;
	}
	
	@JsonIgnore
	protected Date getSessionExpiryDate() {
		return SessionSummary.EXPIRES_UNKNOWN;
		
	}
}
