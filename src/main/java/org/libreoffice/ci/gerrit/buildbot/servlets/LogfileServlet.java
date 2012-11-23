package org.libreoffice.ci.gerrit.buildbot.servlets;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.libreoffice.ci.gerrit.buildbot.config.BuildbotConfig;

import com.google.gerrit.httpd.HtmlDomUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LogfileServlet extends HttpServlet {
	private static final long serialVersionUID = -500973237863842149L;
	@Inject
	BuildbotConfig config;

	@Override
	protected void doGet(final HttpServletRequest req,
			final HttpServletResponse rsp) throws IOException, ServletException {
		doPost(req, rsp);
	}

	@Override
	protected void doPost(final HttpServletRequest req,
			final HttpServletResponse rsp) throws IOException, ServletException {
		rsp.setHeader("Expires", "Fri, 01 Jan 1980 00:00:00 GMT");
		rsp.setHeader("Pragma", "no-cache");
		rsp.setHeader("Cache-Control", "no-cache, must-revalidate");
		rsp.setContentType("text/html");
		rsp.setCharacterEncoding(HtmlDomUtil.ENC);
		final Writer out = rsp.getWriter();

		String fileName = req.getParameter("file");
		File logFile = new File(config.getLogDir() + File.separator + fileName);
		if (!logFile.exists()) {
			out.write("<html>");
			out.write("<body>");
			out.write("<h2>File not found</h2>");
			out.write("</body>");
			out.write("</html>");
			out.close();
			return;
		}

		if (req.getParameter("full") != null) {
			printFull(out, logFile);
		} else {
			printTail(out, fileName, logFile);
		}

		out.close();
	}

	private void printFull(Writer out, File logFile) throws IOException {
		FileInputStream fis = new FileInputStream(logFile);
		InputStreamReader isr = new InputStreamReader(fis, HtmlDomUtil.ENC);
		BufferedReader reader = new BufferedReader(isr);
		int sChunk = 8192;
		char[] buffer = new char[sChunk];
		int length;
		out.write("<pre>");
		while (-1 != (length = reader.read(buffer))) {
			out.write(buffer, 0, length);
		}
		out.write("</pre>");
		reader.close();
	}

	private void printTail(final Writer out, String fileName, File logFile)
			throws IOException, UnsupportedEncodingException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(logFile, "r");
		long size = randomAccessFile.length();
		long skipSize = 400;
		if (size > skipSize) {
			randomAccessFile.seek(size - skipSize);
			randomAccessFile.readLine();
			out.write("... skipped " + randomAccessFile.getFilePointer() / 1024
					+ " KB <a href=\"/plugins/buildbot-1.0/log?file="
					+ fileName + "&full=true\">Full log</a></br>");
		}
		int sizeToRead = (int) (size - skipSize - 1);
		byte[] buffer = new byte[sizeToRead];
		randomAccessFile.read(buffer);
		String str = new String(buffer, HtmlDomUtil.ENC);
		out.write("<pre>");
		out.write(str);
		out.write("</pre>");
		randomAccessFile.close();
	}

}