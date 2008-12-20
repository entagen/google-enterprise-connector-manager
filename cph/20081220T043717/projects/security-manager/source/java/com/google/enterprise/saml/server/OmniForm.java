// Copyright (C) 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.saml.server;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

class OmniForm {
  private final List<AuthSite> sites;
  private final String actionUrl;
  private StringBuffer formContent;
  
  public OmniForm(String configFile, String actionUrl) throws NumberFormatException, IOException {
    sites = AuthSite.getSites(configFile);
    this.actionUrl = actionUrl;
  }

  private void writeHeader() {
    // We expect this form to be posted back to the same URL from which the form
    // was GETed, so skip "action" attribute. 
    formContent = new StringBuffer("<form method=\"post\" name=\"omni\" action=\"" +
                                   actionUrl +
                                   "\">\n");
  }
  private void writeFooter() {
    formContent.append("<input type=\"submit\"></form>");
  }
 
  /*
   * Each area on the omniform looks like:
   *  "Please login to %s@%s", AuthName, HTTPServerName
   *  "username", "input type=text name=u"
   *  "password", "input type=password name=pw"
   */
  private void writeArea(AuthSite site, int idx, UserIdentity id) {
    String inputUserName = "u" + idx;
    String inputPassName = "pw" + idx;
    String inputStatus = "";
    if ((id != null) && id.isVerified()) {
      inputStatus = " disabled";
      formContent.append("<span style=\"color:green\">Logged in to ");
    } else {
      formContent.append("Please login to ");
    }
    formContent.append("\"" + site.getRealm() + "\" @" +
          site.getHostname() + ":<br>\n");
    if ((id != null) && id.isVerified()) {
      formContent.append("</span>");
    }
    formContent.append("<b>Username</b> <input type=\"text\" name=" + inputUserName + inputStatus + "><br>");
    formContent.append("<b>Password</b> <input type=\"password\" name=" + inputPassName + inputStatus + "><br>");
    formContent.append("\n<br><br><br>\n");
  }

  /**
   * 
   * @param ids
   * @return the whole form suitable for display
   */
  public String writeForm(UserIdentity[] ids) {
    int idx = 0;

    writeHeader();
    for (AuthSite site : sites) {
      writeArea(site, idx, (ids != null) ? ids[idx] : null);
      idx++;
    }
    writeFooter();
    return formContent.toString();
  }
  
  /**
   * Parse the form into an array of credentials awaiting authn decision.
   */
  public UserIdentity[] parse(HttpServletRequest request, UserIdentity[] oldIds) {
    String username;
    String password;
    UserIdentity[] identities = new UserIdentity[sites.size()];
    int idx = 0;
    
    for (AuthSite site : sites) {
      username = request.getParameter("u" + idx);
      password = request.getParameter("pw" + idx);
      if (username != null && username.length() > 0 && password != null && password.length() > 0)
        identities[idx] = new UserIdentity(username, password, site);
      else
        identities[idx] = (oldIds == null ? null : oldIds[idx]);
      idx++;
    }
    return identities;
  }

}