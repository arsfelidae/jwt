/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import eu.webtoolkit.jwt.Configuration;
import eu.webtoolkit.jwt.WResource;
import eu.webtoolkit.jwt.WtServlet;
import eu.webtoolkit.jwt.Utils;

/**
 * A WebRequest which wraps the HttpServletRequest to add support for file uploads and
 * testing.
 * <p>
 * WebRequest is used instead of HttpServletRequest inside JWt's request handling,
 * and also in {@link WResource#handleRequest(WebRequest request, WebResponse response)}.
 * It handles files being POST'ed, and treats parameters in the URL or within the
 * request body in the same way.
 * <p>
 * @see WebResponse
 */
public class WebRequest extends HttpServletRequestWrapper {
	private HttpServletRequest httpRequest;
	
	/**
	 * The type of response that this request will need.
	 * 
	 * This is an internal JWt enumeration.
	 */
	public enum ResponseType {
		/**
		 * Renders as an HTML page.
		 */
		Page, 
		/**
		 * Renders as a JavaScript script resource.
		 */
		Script,
		/**
		 * Renders as an Ajax (JavaScript) update.
		 */
		Update
	};

	/**
	 * Enumeration for a WebSocket read event (not yet implemented).
	 */
	public enum ReadEvent {
	}

	/**
	 * Progress listener interface.
	 */
	public interface ProgressListener {
		/**
		 * Update callback method.
		 */
		public void update(WebRequest request, long pBytesRead, long pContentLength);
	}
	
	private Map<String, String[]> parameters_;
	private Map<String, List<UploadedFile>> files_;
	private String scriptName;
	private String pathInfo;

	public static String computeScriptName(HttpServletRequest request, Configuration configuration) {
		String scriptName = request.getServletPath();
	
		if (request.getContextPath() != null)
			scriptName = request.getContextPath() + scriptName;

		if (!scriptName.startsWith("/"))
			scriptName = "/" + scriptName;

		// Jetty will auto-redirect in this case to .../
		// I am not sure if this is according to the servlet spec ?
		if (request.getServletPath().length() == 0 && !scriptName.endsWith("/"))
			scriptName += "/"; 
		
		if (configuration.internalDeploymentSize() != 0) {
			String pathInfo = getPathInfo(request, scriptName); 
			// Move part of the internal path to the script name
			if (pathInfo != null) {
				for (int i = 0; i < configuration.internalDeploymentSize(); ++i) {
					if (pathInfo.length() > 1) {
						int slashPos = pathInfo.indexOf('/', 1);
						if (slashPos == -1)
							slashPos = pathInfo.length();
						if (slashPos != -1) {
							scriptName += pathInfo.substring(0, slashPos);
							pathInfo = pathInfo.substring(slashPos);
						}
					}
				}
			}
			if (!scriptName.endsWith("/"))
				scriptName += "/";
		}
		
		return scriptName;
	}
	
	public static String computePathInfo(HttpServletRequest request, Configuration configuration) {
		String scriptName = computeScriptName(request, configuration);
		return computePathInfo(request, scriptName, configuration);
	}
	
	public static String computePathInfo(HttpServletRequest request, String scriptName, Configuration configuration) {
		String pathInfo = getPathInfo(request, scriptName);
		
		if (configuration.internalDeploymentSize() != 0) {
			// Move part of the internal path to the script name
			for (int i = 0; i < configuration.internalDeploymentSize(); ++i) {
				if (pathInfo.length() > 1) {
					int slashPos = pathInfo.indexOf('/', 1);
					if (slashPos == -1)
						slashPos = pathInfo.length();
					if (slashPos != -1) {
						pathInfo = pathInfo.substring(slashPos);
					}
				}
			}
			if (scriptName.endsWith("/") && pathInfo.startsWith("/"))
				pathInfo = pathInfo.substring(1);
		}
		
		return pathInfo;
	}

	private static String getPathInfo(HttpServletRequest request, String scriptName) {
		String pathInfo = request.getPathInfo();
		
		// Jetty will report "/" as an internal path. Which totally makes no sense but is according
		// to the spec
		if (request.getServletPath().length() == 0)
			if (pathInfo != null && pathInfo.equals("/"))
				pathInfo = "";

		if (pathInfo == null || pathInfo.length() == 0) {
			// Work around (jetty) bug where path info is not interpreted correctly for a URL
			// like /bla/hello;jsessionid=q0f2lqgeivq9uipxwk12gj6s/wt-resources/webkit-transitions.css

			String uri = request.getRequestURI();
			if (uri.startsWith(scriptName + ";")) {
				int pathInfoStart = uri.indexOf('/', scriptName.length() + 1);
				if (pathInfoStart != -1)
					pathInfo = uri.substring(pathInfoStart);
			}
		}

		if (pathInfo == null)
			pathInfo = "";
		return pathInfo;
	}
	
	private void computePaths(Configuration configuration) {
		/*
		 * We compute this here since sometimes when reposted through async (servlet 3), the context and everything
		 * gets messed up (You, JETTY 8!)
		 */
		this.scriptName = computeScriptName(httpRequest, configuration);
		this.pathInfo = computePathInfo(httpRequest, this.scriptName, configuration);
	}
	
	/**
	 * Creates a WebRequest by wrapping an HttpServletRequest
	 * @param request The request to be wrapped.
	 * @param progressListener a progress listener implementation
	 * @param configuration 
	 */
	public WebRequest(HttpServletRequest request, ProgressListener progressListener, Configuration configuration) {
		super(request);
		this.httpRequest  = request;
		
		computePaths(configuration);

		try {
			parse(progressListener);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a mock WebRequest given list of parameters and a list of POST'ed files.
	 * 
	 * @param parameters a list of request parameters
	 * @param files a list of POST'ed files
	 */
	public WebRequest(Map<String, String[]> parameters, Map<String, List<UploadedFile>> files) {
		super(WtServlet.getServletApi().getMockupHttpServletRequest());
		parameters_ = parameters;
		files_ = files;
	}
	
	/**
	 * Returns the request method.
	 */
	public String getRequestMethod() {
		return this.getMethod();
	}

	/**
	 * Returns the script name.
	 * <p>
	 * This returns in principle {@link #getContextPath()} + {@link #getServletPath()}, but
	 * with workaround code for corner cases and container workarounds.
	 * 
	 * @return the url at which the application is deployed
	 */
	public String getScriptName() {
		return scriptName;
	}

	/**
	 * Returns a header value.
	 * <p>
	 * Returns the corresponding header value, using {@link #getHeader(String)} or 
	 * <code>null</code> if the header value is not present
	 * 
	 * @param header the header name
	 * @return the header value, or <code>null</code>.
	 */
	public String getHeaderValue(String header) {
		if (header.equals("Client-IP"))
			return getRemoteAddr();

		return getHeader(header);
	}
	  
	/**
	  * Accesses to specific header fields (calls getHeaderValue()).
	  */
	public String getUserAgent() {
		String v = getHeaderValue("User-Agent");
		return v != null ? v : "";
	}

	/**
	 * Returns the internal path information.
	 * <p>
	 * Returns the {@link HttpServletRequestWrapper#getPathInfo()} or the empty string
	 * if there is no internal path in the request. This method also uses workarounds
	 * for corner cases for some servlet containers.
	 * 
	 * @return the internal path information, or an empty string if there is no internal path.
	 */
	public String getPathInfo() {
		return pathInfo;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void parse(final ProgressListener progressUpdate) throws IOException {
		if (FileUploadBase.isMultipartContent(this)) {
			try {
				// Create a factory for disk-based file items
				DiskFileItemFactory factory = new DiskFileItemFactory();

				// Create a new file upload handler
				ServletFileUpload upload = new ServletFileUpload(factory);

				if (progressUpdate != null) {
					upload.setProgressListener(new org.apache.commons.fileupload.ProgressListener(){
						public void update(long pBytesRead, long pContentLength, int pItems) {
							progressUpdate.update(WebRequest.this, pBytesRead, pContentLength);
						}
					});
				}

				// Parse the request
				List items = upload.parseRequest(this);

				parseParameters();
				
				Iterator itr = items.iterator();

				FileItem fi;
				File f = null;
				while (itr.hasNext()) {
					fi = (FileItem) itr.next();

					// Check if not form field so as to only handle the file inputs
					// else condition handles the submit button input
					if (!fi.isFormField()) {
						try {
							f = File.createTempFile("jwt", "jwt");
							fi.write(f);
							fi.delete();
						} catch (IOException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}

						List<UploadedFile> files = files_.get(fi.getFieldName());
						if (files == null) {
							files = new ArrayList<UploadedFile>();
							files_.put(fi.getFieldName(), files);
						}
						files.add(new UploadedFile(f.getAbsolutePath(), fi.getName(), fi.getContentType()));
					} else {
						String[] v = parameters_.get(fi.getFieldName());
						if (v == null)
							v = new String[1];
						else {
							String[] newv = new String[v.length + 1];
							for (int i = 0; i < v.length; ++i)
								newv[i] = v[i];
							v = newv;
						}
						v[v.length - 1] = fi.getString();
						parameters_.put(fi.getFieldName(), v);
					}
				}
			} catch (FileUploadException e) {
				e.printStackTrace();
			}
		} else
			parseParameters();
	}

	private void parseParameters() throws IOException {
		Map<String, String[]> parameterMap = super.getParameterMap();

		parameters_ = new HashMap<String, String []>(parameterMap);
		files_ = new HashMap<String, List<UploadedFile>>();
		
		String[] paramContentType = parameters_.get("contentType"); 

		if (paramContentType != null && paramContentType[0].equals("x-www-form-urlencoded") && this.getContentType() == null) {
			byte[] buf = new byte[this.getContentLength()];
			this.getInputStream().read(buf);
			String[] pairs = new String(buf, "UTF-8").split("\\&");

			for (int i = 0; i < pairs.length; i++) {
		      String[] fields = {"", ""};
		      String[] pair = pairs[i].split("=");
		      for (int j = 0; j < pair.length && j < 2; j++)
		    	  fields[j] = URLDecoder.decode(pair[j], "UTF-8");
		      if (!fields[0].equals(""))
		    	  parameters_.put(fields[0], new String [] { fields[1] });
		    }
		}

		String[] wtParamsAr = parameters_.get("Wt-params");
		if (wtParamsAr != null) {
			String wtParams = wtParamsAr[0];
			Utils.parseFormUrlEncoded(wtParams, parameters_);
		}
	}
	
	/**
	 * Returns the list of uploaded files.
	 * 
	 * @return the list of uploaded files.
	 */
	public Map<String, List<UploadedFile>> getUploadedFiles() {
		return files_;
	}

	/**
	 * Returns the parameter map.
	 * <p>
	 * The parameter map includes both the parameters from the query string, as well
	 * as parameters posted in the body.
	 */
	public Map<String, String[]> getParameterMap() {
		return parameters_;
	}

	/**
	 * Returns the parameter values for a parameter.
	 * <p>
	 * Returns an array of parameters values given for a particular parameter. When
	 * no parameter value was assigned to the parameter, an empty array is returned.
	 * 
	 * @see #getParameterMap()
	 */
	@Override
	public String[] getParameterValues(String name) {
		if (parameters_.containsKey(name))
			return parameters_.get(name);
		else
			return new String[0];
	}

	/**
	 * Returns the parameter value for a parameter's name.
	 * 
	 * @see javax.servlet.ServletRequestWrapper#getParameter(java.lang.String)
	 */
	@Override
	public String getParameter(String name) {
		if (parameters_ == null)
			try {
				parseParameters();
			} catch (IOException e) {
				e.printStackTrace();
			}

		if (parameters_.containsKey(name)) {
			String[] paramList = parameters_.get(name);
			if (paramList.length > 0 && paramList[0] != null) 
				return paramList[0];
		}
		
		return null;
	}

	/**
	 * Returns whether this request is a WebSocket request.
	 * 
	 * This is an internal JWt method.
	 */
	public boolean isWebSocketRequest() {
		return false;
	}

	/**
	 * Returns whether this request is a WebSocket message.
	 * 
	 * This is an internal JWt method.
	 */
	public boolean isWebSocketMessage() {
		return false;
	}
}
