/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sysds.runtime.controlprogram.federated.monitoring.controllers;

import io.netty.handler.codec.http.FullHttpResponse;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.WorkerModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.Request;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.Response;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.services.MapperService;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.services.StatisticsService;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.services.WorkerService;

public class WorkerController implements IController {
	private final WorkerService workerService = new WorkerService();

	@Override
	public FullHttpResponse create(Request request) {

		var model = MapperService.getModelFromBody(request, WorkerModel.class);

		model.id = workerService.create(model);
		model.setOnlineStatus(this.getWorkerOnlineStatus(model.id, model.address));

		return Response.ok(model.toString());
	}

	@Override
	public FullHttpResponse update(Request request, Long objectId) {
		var model = MapperService.getModelFromBody(request, WorkerModel.class);

		workerService.update(model);
		model.setOnlineStatus(this.getWorkerOnlineStatus(model.id, model.address));

		return Response.ok(model.toString());
	}

	@Override
	public FullHttpResponse delete(Request request, Long objectId) {
		workerService.remove(objectId);

		return Response.ok(Constants.GENERIC_SUCCESS_MSG);
	}

	@Override
	public FullHttpResponse get(Request request, Long objectId) {
		var result = workerService.get(objectId);

		if (result == null) {
			return Response.notFound(Constants.NOT_FOUND_MSG);
		}

		result.setOnlineStatus(this.getWorkerOnlineStatus(result.id, result.address));

		return Response.ok(result.toString());
	}

	@Override
	public FullHttpResponse getAll(Request request) {
		var workers = workerService.getAll();

		for (var worker: workers) {
			worker.setOnlineStatus(this.getWorkerOnlineStatus(worker.id, worker.address));
		}

		return Response.ok(workers.toString());
	}

	private boolean getWorkerOnlineStatus(Long workerId, String workerAddress) {
		var stats = StatisticsService.getWorkerStatistics(workerId, workerAddress);

		return stats != null;
	}
}
