// Copyright 2012 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.libreoffice.ci.gerrit.buildbot.servlets;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;

import javax.servlet.http.HttpServletRequest;

/** Type of formatting to use in the response to the client. */
public enum RenderType {
  HTML("text/html"),
  TEXT("text/plain"),
  JSON("application/json"),
  DEFAULT("*/*");

  private static final String FORMAT_TYPE_ATTRIBUTE = RenderType.class.getName();

  public static RenderType getFormatType(HttpServletRequest req) {
    RenderType result = (RenderType) req.getAttribute(FORMAT_TYPE_ATTRIBUTE);
    if (result != null) {
      return result;
    }

    String format = req.getParameter("format");
    if (format != null) {
      for (RenderType type : RenderType.values()) {
        if (format.equalsIgnoreCase(type.name())) {
          return set(req, type);
        }
      }
      throw new IllegalArgumentException("Invalid format " + format);
    }

    String accept = req.getHeader(HttpHeaders.ACCEPT);
    if (Strings.isNullOrEmpty(accept)) {
      return set(req, DEFAULT);
    }

    for (String p : accept.split("[ ,;][ ,;]*")) {
      for (RenderType type : RenderType.values()) {
        if (p.equals(type.mimeType)) {
          return set(req, type != HTML ? type : DEFAULT);
        }
      }
    }
    return set(req, DEFAULT);
  }

  private static RenderType set(HttpServletRequest req, RenderType format) {
    req.setAttribute(FORMAT_TYPE_ATTRIBUTE, format);
    return format;
  }

  private final String mimeType;

  private RenderType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getMimeType() {
    return mimeType;
  }
}
