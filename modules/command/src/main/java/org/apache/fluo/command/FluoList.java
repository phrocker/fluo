/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.fluo.command;

import java.util.Collections;
import java.util.List;

import com.beust.jcommander.Parameters;
import org.apache.curator.framework.CuratorFramework;
import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.fluo.core.client.FluoAdminImpl;
import org.apache.fluo.core.util.CuratorUtil;

@Parameters(commandNames = "list",
    commandDescription = "Lists all Fluo applications in Fluo instance")
public class FluoList extends ConfigCommand {

  @Override
  public void execute() throws FluoCommandException {
    FluoConfiguration config = getConfig();

    try (CuratorFramework curator = CuratorUtil.newFluoCurator(config)) {
      curator.start();

      if (!checkCuratorExists(curator)) {
        System.out.println("Fluo instance (" + config.getInstanceZookeepers() + ") has not been "
            + "created yet in Zookeeper.  It will be created when the first Fluo application is "
            + "initialized for this instance.");
        return;
      }
      List<String> children = getCuratorChildren(curator);
      if (children.isEmpty()) {
        System.out.println("Fluo instance (" + config.getInstanceZookeepers() + ") does not "
            + "contain any Fluo applications.");
        return;
      }
      Collections.sort(children);

      System.out.println("Fluo instance (" + config.getInstanceZookeepers() + ") contains "
          + children.size() + " application(s)\n");
      System.out.println("Application     Status     # Workers");
      System.out.println("-----------     ------     ---------");

      for (String path : children) {
        listApp(config, path);
      }
    }
  }

  private boolean checkCuratorExists(CuratorFramework curator) {
    try {
      return curator.checkExists().forPath("/") != null;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      // throwing RuntimeException so stack trace is printed on command line
      throw new RuntimeException("Error getting curator children", e);
    }
  }

  private List<String> getCuratorChildren(CuratorFramework curator) {
    try {
      return curator.getChildren().forPath("/");
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      // throwing RuntimeException so stack trace is printed on command line
      throw new RuntimeException("Error getting curator children", e);
    }
  }

  private void listApp(FluoConfiguration config, String path) {
    FluoConfiguration appConfig = new FluoConfiguration(config);
    appConfig.setApplicationName(path);
    try (FluoAdminImpl admin = new FluoAdminImpl(appConfig)) {
      String state = "STOPPED";
      if (admin.applicationRunning()) {
        state = "RUNNING";
      }
      int numWorkers = admin.numWorkers();
      System.out.format("%-15s %-11s %4d\n", path, state, numWorkers);
    }
  }
}
