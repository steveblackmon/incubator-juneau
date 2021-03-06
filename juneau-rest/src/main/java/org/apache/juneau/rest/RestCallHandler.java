// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest;

import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.internal.StringUtils.*;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.vars.*;

/**
 * Class that handles the basic lifecycle of an HTTP REST call.
 * <p>
 * Subclasses can override these methods to tailor how HTTP REST calls are handled.
 * Subclasses MUST implement a public constructor that takes in a {@link RestContext} object.
 * <p>
 * RestCallHandlers are associated with servlets/resources in one of the following ways:
 * <ul>
 * 	<li>The {@link RestResource#callHandler @RestResource.callHandler()} annotation.
 * 	<li>The {@link RestConfig#setCallHandler(Class)} method.
 * </ul>
 */
public class RestCallHandler {

	private final RestContext context;
	private final RestLogger logger;
	private final RestServlet restServlet;
	private final Map<String,CallRouter> callRouters;

	/**
	 * Constructor.
	 * @param context The resource context.
	 */
	public RestCallHandler(RestContext context) {
		this.context = context;
		this.logger = context.getLogger();
		this.callRouters = context.getCallRouters();
		this.restServlet = context.getRestServlet();  // Null if this isn't a RestServlet!
	}

	/**
	 * Creates a {@link RestRequest} object based on the specified incoming {@link HttpServletRequest} object.
	 * <p>
	 * Subclasses may choose to override this method to provide a specialized request object.
	 *
	 * @param req The request object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped request object.
	 * @throws ServletException If any errors occur trying to interpret the request.
	 */
	protected RestRequest createRequest(HttpServletRequest req) throws ServletException {
		return new RestRequest(context, req);
	}

	/**
	 * Creates a {@link RestResponse} object based on the specified incoming {@link HttpServletResponse} object
	 * 	and the request returned by {@link #createRequest(HttpServletRequest)}.
	 * <p>
	 * Subclasses may choose to override this method to provide a specialized response object.
	 *
	 * @param req The request object returned by {@link #createRequest(HttpServletRequest)}.
	 * @param res The response object from the {@link #service(HttpServletRequest, HttpServletResponse)} method.
	 * @return The wrapped response object.
	 * @throws ServletException If any errors occur trying to interpret the request or response.
	 */
	protected RestResponse createResponse(RestRequest req, HttpServletResponse res) throws ServletException {
		return new RestResponse(context, req, res);
	}

	/**
	 * The main service method.
	 * <p>
	 * Subclasses can optionally override this method if they want to tailor the behavior of requests.
	 *
	 * @param r1 The incoming HTTP servlet request object.
	 * @param r2 The incoming HTTP servlet response object.
	 * @throws ServletException
	 * @throws IOException
	 */
	protected void service(HttpServletRequest r1, HttpServletResponse r2) throws ServletException, IOException {

		logger.log(FINE, "HTTP: {0} {1}", r1.getMethod(), r1.getRequestURI());
		long startTime = System.currentTimeMillis();

		try {
			context.checkForInitException();

			String pathInfo = RestUtils.getPathInfoUndecoded(r1);  // Can't use r1.getPathInfo() because we don't want '%2F' resolved.

			// If this resource has child resources, try to recursively call them.
			if (pathInfo != null && context.hasChildResources() && (! pathInfo.equals("/"))) {
				int i = pathInfo.indexOf('/', 1);
				String pathInfoPart = i == -1 ? pathInfo.substring(1) : pathInfo.substring(1, i);
				RestContext childResource = context.getChildResource(pathInfoPart);
				if (childResource != null) {
					final String pathInfoRemainder = (i == -1 ? null : pathInfo.substring(i));
					final String servletPath = r1.getServletPath() + "/" + pathInfoPart;
					final HttpServletRequest childRequest = new HttpServletRequestWrapper(r1) {
						@Override /* ServletRequest */
						public String getPathInfo() {
							return urlDecode(pathInfoRemainder);
						}
						@Override /* ServletRequest */
						public String getServletPath() {
							return servletPath;
						}
					};
					childResource.getCallHandler().service(childRequest, r2);
					return;
				}
			}

			RestRequest req = createRequest(r1);
			RestResponse res = createResponse(req, r2);
			String method = req.getMethod();
			String methodUC = method.toUpperCase(Locale.ENGLISH);

			StreamResource r = null;
			if (pathInfo != null) {
				String p = pathInfo.substring(1);
				if (p.equals("favicon.ico"))
					r = context.getFavIcon();
				else if (p.equals("style.css"))
					r = context.getStyleSheet();
				else if (context.isStaticFile(p))
					r = context.resolveStaticFile(p);
			}

			if (r != null) {
				res.setStatus(SC_OK);
				res.setOutput(r);
			} else {
				// If the specified method has been defined in a subclass, invoke it.
				int rc = SC_METHOD_NOT_ALLOWED;
				if (callRouters.containsKey(methodUC)) {
					rc = callRouters.get(methodUC).invoke(pathInfo, req, res);
				} else if (callRouters.containsKey("*")) {
					rc = callRouters.get("*").invoke(pathInfo, req, res);
				}

				// If not invoked above, see if it's an OPTIONs request
				if (rc != SC_OK)
					handleNotFound(rc, req, res);
			}

			if (res.hasOutput()) {
				Object output = res.getOutput();

				// Do any class-level transforming.
				for (RestConverter converter : context.getConverters())
					output = converter.convert(req, output, context.getBeanContext().getClassMetaForObject(output));

				res.setOutput(output);

				// Now serialize the output if there was any.
				// Some subclasses may write to the OutputStream or Writer directly.
				handleResponse(req, res, output);
			}

			onSuccess(req, res, System.currentTimeMillis() - startTime);

			// Make sure our writer in RestResponse gets written.
			res.flushBuffer();

		} catch (RestException e) {
			handleError(r1, r2, e);
		} catch (Throwable e) {
			handleError(r1, r2, new RestException(SC_INTERNAL_SERVER_ERROR, e));
		}
		logger.log(FINE, "HTTP: [{0} {1}] finished in {2}ms", r1.getMethod(), r1.getRequestURI(), System.currentTimeMillis()-startTime);
	}

	/**
	 * The main method for serializing POJOs passed in through the {@link RestResponse#setOutput(Object)} method or returned by
	 * the Java method.
	 * <p>
	 * Subclasses may override this method if they wish to modify the way the output is rendered or support
	 * 	other output formats.
	 * <p>
	 * The default implementation simply iterates through the response handlers on this resource
	 * looking for the first one whose {@link ResponseHandler#handle(RestRequest, RestResponse, Object)} method returns <jk>true</jk>.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param output The output to serialize in the response.
	 * @throws IOException
	 * @throws RestException
	 */
	protected void handleResponse(RestRequest req, RestResponse res, Object output) throws IOException, RestException {
		// Loop until we find the correct handler for the POJO.
		for (ResponseHandler h : context.getResponseHandlers())
			if (h.handle(req, res, output))
				return;
		throw new RestException(SC_NOT_IMPLEMENTED, "No response handlers found to process output of type '"+(output == null ? null : output.getClass().getName())+"'");
	}

	/**
	 * Handle the case where a matching method was not found.
	 * <p>
	 * Subclasses can override this method to provide a 2nd-chance for specifying a response.
	 * The default implementation will simply throw an exception with an appropriate message.
	 *
	 * @param rc The HTTP response code.
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @throws Exception
	 */
	protected void handleNotFound(int rc, RestRequest req, RestResponse res) throws Exception {
		String pathInfo = req.getPathInfo();
		String methodUC = req.getMethod();
		String onPath = pathInfo == null ? " on no pathInfo"  : String.format(" on path '%s'", pathInfo);
		if (rc == SC_NOT_FOUND)
			throw new RestException(rc, "Method ''{0}'' not found on resource with matching pattern{1}.", methodUC, onPath);
		else if (rc == SC_PRECONDITION_FAILED)
			throw new RestException(rc, "Method ''{0}'' not found on resource{1} with matching matcher.", methodUC, onPath);
		else if (rc == SC_METHOD_NOT_ALLOWED)
			throw new RestException(rc, "Method ''{0}'' not found on resource.", methodUC);
		else
			throw new ServletException("Invalid method response: " + rc);
	}

	/**
	 * Method for handling response errors.
	 * <p>
	 * The default implementation logs the error and calls {@link #renderError(HttpServletRequest,HttpServletResponse,RestException)}.
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	protected synchronized void handleError(HttpServletRequest req, HttpServletResponse res, RestException e) throws IOException {
		e.setOccurrence(context == null ? 0 : context.getStackTraceOccurrence(e));
		logger.onError(req, res, e);
		renderError(req, res, e);
	}

	/**
	 * Method for rendering response errors.
	 * <p>
	 * The default implementation renders a plain text English message, optionally with a stack trace
	 * 	if {@link RestContext#REST_renderResponseStackTraces} is enabled.
	 * <p>
	 * Subclasses can override this method to provide their own custom error response handling.
	 *
	 * @param req The servlet request.
	 * @param res The servlet response.
	 * @param e The exception that occurred.
	 * @throws IOException Can be thrown if a problem occurred trying to write to the output stream.
	 */
	protected void renderError(HttpServletRequest req, HttpServletResponse res, RestException e) throws IOException {

		int status = e.getStatus();
		res.setStatus(status);
		res.setContentType("text/plain");
		res.setHeader("Content-Encoding", "identity");

		Throwable t = e.getRootCause();
		if (t != null) {
			res.setHeader("Exception-Name", t.getClass().getName());
			res.setHeader("Exception-Message", t.getMessage());
		}

		PrintWriter w = null;
		try {
			w = res.getWriter();
		} catch (IllegalStateException e2) {
			w = new PrintWriter(new OutputStreamWriter(res.getOutputStream(), IOUtils.UTF8));
		}
		String httpMessage = RestUtils.getHttpResponseText(status);
		if (httpMessage != null)
			w.append("HTTP ").append(String.valueOf(status)).append(": ").append(httpMessage).append("\n\n");
		if (context != null && context.isRenderResponseStackTraces())
			e.printStackTrace(w);
		else
			w.append(e.getFullStackMessage(true));
		w.flush();
		w.close();
	}

	/**
	 * Callback method for listening for successful completion of requests.
	 * <p>
	 * Subclasses can override this method for gathering performance statistics.
	 * <p>
	 * The default implementation does nothing.
	 *
	 * @param req The HTTP request.
	 * @param res The HTTP response.
	 * @param time The time in milliseconds it took to process the request.
	 */
	protected void onSuccess(RestRequest req, RestResponse res, long time) {
		if (restServlet != null)
			restServlet.onSuccess(req, res, time);
	}

	/**
	 * Callback method that gets invoked right before the REST Java method is invoked.
	 * <p>
	 * Subclasses can override this method to override request headers or set request-duration properties
	 * 	before the Java method is invoked.
	 *
	 * @param req The HTTP servlet request object.
	 * @throws RestException If any error occurs.
	 */
	protected void onPreCall(RestRequest req) throws RestException {
		if (restServlet != null)
			restServlet.onPreCall(req);
	}

	/**
	 * Callback method that gets invoked right after the REST Java method is invoked, but before
	 * 	the serializer is invoked.
	 * <p>
	 * Subclasses can override this method to override request and response headers, or
	 * 	set/override properties used by the serializer.
	 *
	 * @param req The HTTP servlet request object.
	 * @param res The HTTP servlet response object.
	 * @throws RestException If any error occurs.
	 */
	protected void onPostCall(RestRequest req, RestResponse res) throws RestException {
		if (restServlet != null)
			restServlet.onPostCall(req, res);
	}

	/**
	 * Returns the session objects for the specified request.
	 * <p>
	 * The default implementation simply returns a single map containing <code>{'req':req}</code>.
	 *
	 * @param req The REST request.
	 * @return The session objects for that request.
	 */
	public Map<String,Object> getSessionObjects(RestRequest req) {
		Map<String,Object> m = new HashMap<String,Object>();
		m.put(RequestVar.SESSION_req, req);
		return m;
	}
}
