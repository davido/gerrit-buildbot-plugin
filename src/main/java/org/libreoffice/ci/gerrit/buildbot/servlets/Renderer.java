package org.libreoffice.ci.gerrit.buildbot.servlets;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.template.soy.tofu.SoyTofu;

/** Renderer for Soy templates used by Buildbot. */
public abstract class Renderer {
  private static final List<String> SOY_FILENAMES = ImmutableList.of(
      "Common.soy",
      "Queue.soy");

  public static final Map<String, String> STATIC_URL_GLOBALS = ImmutableMap.of(
      "buildbot.CSS_URL", "/plugins/buildbot/static/buildbot.css");

  protected static final URL toFileURL(String filename) {
    if (filename == null) {
      return null;
    }
    try {
      return new File(filename).toURI().toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  protected ImmutableList<URL> templates;
  protected ImmutableMap<String, String> globals;

  protected Renderer(Function<String, URL> resourceMapper, Map<String, String> globals,
      URL customTemplates, String siteTitle) {
    List<URL> allTemplates = Lists.newArrayListWithCapacity(SOY_FILENAMES.size() + 1);
    for (String filename : SOY_FILENAMES) {
      allTemplates.add(resourceMapper.apply(filename));
    }
    if (customTemplates != null) {
      allTemplates.add(customTemplates);
    } else {
      allTemplates.add(resourceMapper.apply("DefaultCustomTemplates.soy"));
    }
    templates = ImmutableList.copyOf(allTemplates);

    Map<String, String> allGlobals = Maps.newHashMap();
    for (Map.Entry<String, String> e : STATIC_URL_GLOBALS.entrySet()) {
      allGlobals.put(e.getKey(), e.getValue());
    }
    allGlobals.put("buildbot.SITE_TITLE", siteTitle);
    allGlobals.putAll(globals);
    this.globals = ImmutableMap.copyOf(allGlobals);
  }

  public void render(HttpServletResponse res, String templateName) throws IOException {
    render(res, templateName, ImmutableMap.<String, Object> of());
  }

  public void render(HttpServletResponse res, String templateName, Map<String, ?> soyData)
      throws IOException {
    res.setContentType("text/html");
    res.setCharacterEncoding("UTF-8");
    byte[] data = newRenderer(templateName).setData(soyData).render().getBytes(Charsets.UTF_8);
    res.setContentLength(data.length);
    res.getOutputStream().write(data);
  }

  SoyTofu.Renderer newRenderer(String templateName) {
    return getTofu().newRenderer(templateName);
  }

  protected abstract SoyTofu getTofu();
}
