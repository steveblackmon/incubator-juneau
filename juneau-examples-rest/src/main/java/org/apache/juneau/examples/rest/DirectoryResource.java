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
package org.apache.juneau.examples.rest;

import static java.util.logging.Level.*;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.rest.RestContext.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Properties;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.utils.*;

/**
 * Sample REST resource for exploring local file systems.
 */
@RestResource(
	messages="nls/DirectoryResource",
	pageLinks="{up:'request:/..',options:'servlet:/?method=OPTIONS',source:'$C{Source/gitHub}/org/apache/juneau/examples/rest/DirectoryResource.java'}",
	properties={
		@Property(name=HTML_uriAnchorText, value=PROPERTY_NAME),
		@Property(name=REST_allowMethodParam, value="*"),
		@Property(name="rootDir", value="$S{java.io.tmpdir}"),
		@Property(name="allowViews", value="false"),
		@Property(name="allowDeletes", value="false"),
		@Property(name="allowPuts", value="false")
	}
)
public class DirectoryResource extends Resource {
	private static final long serialVersionUID = 1L;

	
	private File rootDir;     // The root directory

	// Settings enabled through servlet init parameters
	private boolean allowDeletes, allowPuts, allowViews;

	private static Logger logger = Logger.getLogger(DirectoryResource.class.getName());

	@Override /* RestServlet */
	public synchronized void init(RestConfig config) throws Exception {
		super.init(config);
		ObjectMap p = config.getProperties();
		rootDir = new File(p.getString("rootDir"));
		allowViews = p.getBoolean("allowViews", false);
		allowDeletes = p.getBoolean("allowDeletes", false);
		allowPuts = p.getBoolean("allowPuts", false);
	}

	/** Returns the root directory defined by the 'rootDir' init parameter */
	protected File getRootDir() {
		if (rootDir == null) {
			rootDir = new File(getProperties().getString("rootDir"));
			if (! rootDir.exists())
				if (! rootDir.mkdirs())
					throw new RuntimeException("Could not create root dir");
		}
		return rootDir;
	}

	/** GET request handler */
	@RestMethod(name="GET", path="/*", converters={Queryable.class})
	public Object doGet(RestRequest req, @Properties ObjectMap properties) throws Exception {

		String pathInfo = req.getPathInfo();
		File f = pathInfo == null ? rootDir : new File(rootDir.getAbsolutePath() + pathInfo);

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		properties.put("path", f.getAbsolutePath());

		if (f.isDirectory()) {
			List<FileResource> l = new LinkedList<FileResource>();
			for (File fc : f.listFiles()) {
				URL fUrl = new URL(req.getRequestURL().append("/").append(fc.getName()).toString());
				l.add(new FileResource(fc, fUrl));
			}
			return l;
		}

		return new FileResource(f, new URL(req.getRequestURL().toString()));
	}

	/** DELETE request handler */
	@RestMethod(name="DELETE", path="/*", guards=AdminGuard.class)
	public Object doDelete(RestRequest req) throws Exception {

		if (! allowDeletes)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DELETE not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		deleteFile(f);

		if (req.getHeader("Accept").contains("text/html"))
			return new Redirect();
		return "File deleted";
	}

	/** PUT request handler */
	@RestMethod(name="PUT", path="/*", guards=AdminGuard.class)
	public Object doPut(RestRequest req) throws Exception {

		if (! allowPuts)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "PUT not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());
		String parentSubPath = f.getParentFile().getAbsolutePath().substring(rootDir.getAbsolutePath().length());
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
		IOPipe.create(req.getInputStream(), bos).closeOut().run();
		if (req.getContentType().contains("html"))
			return new Redirect(parentSubPath);
		return "File added";
	}

	/** VIEW request handler (overloaded GET for viewing file contents) */
	@RestMethod(name="VIEW", path="/*")
	public void doView(RestRequest req, RestResponse res) throws Exception {

		if (! allowViews)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "VIEW not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "VIEW not available on directories");

		res.setOutput(new FileReader(f)).setContentType("text/plain");
	}

	/** DOWNLOAD request handler (overloaded GET for downloading file contents) */
	@RestMethod(name="DOWNLOAD")
	public void doDownload(RestRequest req, RestResponse res) throws Exception {

		if (! allowViews)
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DOWNLOAD not enabled");

		File f = new File(rootDir.getAbsolutePath() + req.getPathInfo());

		if (!f.exists())
			throw new RestException(SC_NOT_FOUND, "File not found");

		if (f.isDirectory())
			throw new RestException(SC_METHOD_NOT_ALLOWED, "DOWNLOAD not available on directories");

		res.setOutput(new FileReader(f)).setContentType("application");
	}

	/** File POJO */
	public class FileResource {
		private File f;
		private URL url;

		/** Constructor */
		public FileResource(File f, URL url) {
			this.f = f;
			this.url = url;
		}

		// Bean property getters

		public URL getUrl() {
			return url;
		}

		public String getType() {
			return (f.isDirectory() ? "dir" : "file");
		}

		public String getName() {
			return f.getName();
		}

		public long getSize() {
			return f.length();
		}

		public Date getLastModified() {
			return new Date(f.lastModified());
		}

		public URL getView() throws Exception {
			if (allowViews && f.canRead() && ! f.isDirectory())
				return new URL(url + "?method=VIEW");
			return null;
		}

		public URL getDownload() throws Exception {
			if (allowViews && f.canRead() && ! f.isDirectory())
				return new URL(url + "?method=DOWNLOAD");
			return null;
		}

		public URL getDelete() throws Exception {
			if (allowDeletes && f.canWrite())
				return new URL(url + "?method=DELETE");
			return null;
		}
	}

	/** Utility method */
	private void deleteFile(File f) {
		try {
			if (f.isDirectory())
				for (File fc : f.listFiles())
					deleteFile(fc);
			f.delete();
		} catch (Exception e) {
			logger.log(WARNING, "Cannot delete file '" + f.getAbsolutePath() + "'", e);
		}
	}
}
