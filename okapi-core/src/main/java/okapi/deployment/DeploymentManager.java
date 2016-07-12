/*
 * Copyright (C) 2016 Index Data
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
package okapi.deployment;

import com.codahale.metrics.Timer;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import okapi.bean.DeploymentDescriptor;
import okapi.bean.NodeDescriptor;
import okapi.util.ModuleHandle;
import okapi.bean.Ports;
import okapi.bean.ProcessDeploymentDescriptor;
import okapi.discovery.DiscoveryManager;
import okapi.util.DropwizardHelper;
import okapi.util.ProcessModuleHandle;
import static okapi.util.ErrorType.*;
import okapi.util.ExtendedAsyncResult;
import okapi.util.Failure;
import okapi.util.Success;

/**
 * Manages deployment of modules.
 * This actually spawns processes and allocates ports for modules that are
 * to be run. 
 */
public class DeploymentManager {

  private final Logger logger = LoggerFactory.getLogger("okapi");
  LinkedHashMap<String, DeploymentDescriptor> list = new LinkedHashMap<>();
  Vertx vertx;
  Ports ports;
  String host;
  DiscoveryManager dm;
  private final int listenPort;

  public DeploymentManager(Vertx vertx, DiscoveryManager dm,
          String host, Ports ports, int listenPort) {
    this.dm = dm;
    this.vertx = vertx;
    this.host = host;
    this.listenPort = listenPort;
    this.ports = ports;
  }

  public void init(Handler<ExtendedAsyncResult<Void>> fut) {
    NodeDescriptor nd = new NodeDescriptor();
    nd.setUrl("http://" + host + ":" + listenPort);
    nd.setNodeId(host);
    dm.addNode(nd, fut);
  }

  public void shutdown(Handler<ExtendedAsyncResult<Void>> fut) {
    shutdownR(list.keySet().iterator(), fut);
  }

  private void shutdownR(Iterator<String> it, Handler<ExtendedAsyncResult<Void>> fut) {
    if (!it.hasNext()) {
      fut.handle(new Success<>());
    } else {
      DeploymentDescriptor md = list.get(it.next());
      ModuleHandle mh = md.getModuleHandle();
      mh.stop(future -> {
        shutdownR(it, fut);
      });
    }
  }

  public void deploy(DeploymentDescriptor md1, Handler<ExtendedAsyncResult<DeploymentDescriptor>> fut) {
    String id = md1.getInstId();
    if (id != null) {
      if (list.containsKey(id)) {
        fut.handle(new Failure<>(USER, "already deployed: " + id));
        return;
      }
    }
    String srvc = md1.getSrvcId();
    Timer.Context tim = DropwizardHelper.getTimerContext("deploy." + srvc + ".deploy");

    int use_port = ports.get();
    if (use_port == -1) {
      fut.handle(new Failure<>(INTERNAL, "all ports in use"));
      return;
    }
    String url = "http://" + host + ":" + use_port;

    if (id == null) {
      id = host + "-" + use_port;
      md1.setInstId(id);
    }
    logger.info("deploy instId " + id);
    ProcessDeploymentDescriptor descriptor = md1.getDescriptor();
    ProcessModuleHandle pmh = new ProcessModuleHandle(vertx, descriptor,
            ports, use_port);
    ModuleHandle mh = pmh;
    mh.start(future -> {
      if (future.succeeded()) {
        DeploymentDescriptor md2
                = new DeploymentDescriptor(md1.getInstId(), md1.getSrvcId(),
                        url, md1.getDescriptor(), mh);
        md2.setNodeId(md1.getNodeId() != null ? md1.getNodeId() : host);
        list.put(md2.getInstId(), md2);
        tim.stop();
        dm.add(md2, res -> {
          fut.handle(new Success<>(md2));
        });
      } else {
        tim.stop();
        ports.free(use_port);
        fut.handle(new Failure<>(INTERNAL, future.cause()));
      }
    });
  }

  public void undeploy(String id, Handler<ExtendedAsyncResult<Void>> fut) {
    logger.info("undeploy instId " + id);
    if (!list.containsKey(id)) {
      fut.handle(new Failure<>(NOT_FOUND, "not found: " + id));
    } else {
      Timer.Context tim = DropwizardHelper.getTimerContext("deploy." + id + ".undeploy");
      DeploymentDescriptor md = list.get(id);
      dm.remove(md.getSrvcId(), md.getInstId(), res -> {
        if (res.failed()) {
          tim.close();
          fut.handle(new Failure<>(res.getType(), res.cause()));
        } else {
          ModuleHandle mh = md.getModuleHandle();
          mh.stop(future -> {
            if (future.failed()) {
              fut.handle(new Failure<>(INTERNAL, future.cause()));
            } else {
              fut.handle(new Success<>());
              tim.close();
              list.remove(id);
            }
          });
        }
      });
    }
  }

  public void list(Handler<ExtendedAsyncResult<List<DeploymentDescriptor>>> fut) {
    List<DeploymentDescriptor> ml = new LinkedList<>();
    for (String id : list.keySet()) {
      ml.add(list.get(id));
    }
    fut.handle(new Success<>(ml));
  }

  public void get(String id, Handler<ExtendedAsyncResult<DeploymentDescriptor>> fut) {
    if (!list.containsKey(id)) {
      fut.handle(new Failure<>(NOT_FOUND, "not found: " + id));
    } else {
      fut.handle(new Success<>(list.get(id)));
    }
  }

}
