/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.leadpony.peace;

import java.io.IOException;
import java.io.Writer;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.Constants;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.tomcat.util.res.StringManager;

/**
 * An error reporter which outputs the error objects defined in the JSON:API
 * Specification.
 *
 * @author leadpony
 */
public class JsonApiErrorReportValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        if (!shouldReport(response)) {
            return;
        }

        response.setContentType("application/vnd.api+json");
        response.setCharacterEncoding("UTF-8");

        try {
            Writer writer = response.getReporter();
            writer.write(buildResponseBody(request, response, throwable));
            response.finishResponse();
        } catch (IOException e) {
            // ignores the exception
        }
    }

    private static boolean shouldReport(Response response) {
        final int statusCode = response.getStatus();
        if (statusCode < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return false;
        }
        return true;
    }

    private static String buildResponseBody(Request request, Response response, Throwable thrown) {
        final int statusCode = response.getStatus();
        final String title = getTitle(request, statusCode);

        StringBuilder builder = new StringBuilder();
        builder.append("{\"errors\":[{")
               .append("\"status\":\"").append(statusCode).append("\",")
               .append("\"title\":\"").append(title).append('\"');

        if (thrown != null) {
            String message = thrown.getMessage();
            if (message != null) {
                builder.append(",\"detail\":\"")
                       .append(message)
                       .append("\"");
            }
        }

        return builder.append("}]}").toString();
    }

    private static String getTitle(Request request, int statusCode) {
        StringManager stringManager = StringManager.getManager(
                Constants.Package, request.getLocales());
        return stringManager.getString("http." + statusCode + ".reason");
    }
}
