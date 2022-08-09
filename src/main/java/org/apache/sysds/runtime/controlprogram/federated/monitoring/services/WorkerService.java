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

package org.apache.sysds.runtime.controlprogram.federated.monitoring.services;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.DataObjectModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.RequestModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.WorkerModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.repositories.Constants;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.repositories.DerbyRepository;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.repositories.IRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WorkerService {
	private static final IRepository entityRepository = new DerbyRepository();
	// { workerId, { workerAddress, workerStatus } }
	private static final Map<Long, Pair<String, Boolean>> cachedWorkers = new HashMap<>();

	public WorkerService() {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(syncWorkerStatisticsWithDB(), 0, 3, TimeUnit.SECONDS);
	}

	public Long create(WorkerModel model) {

		long id = entityRepository.createEntity(model);
		model.id = id;

		updateCachedWorkers(List.of(model), false);

		return id;
	}

	public void update(WorkerModel model) {
		entityRepository.updateEntity(model);

		updateCachedWorkers(List.of(model), false);
	}

	public void remove(Long id) {
		entityRepository.removeEntity(id, WorkerModel.class);

		updateCachedWorkers(List.of(new WorkerModel(id)), true);
	}

	public WorkerModel get(Long id) {
		var worker = entityRepository.getEntity(id, WorkerModel.class);

		updateCachedWorkers(List.of(worker), false);

		return worker;
	}

	public List<WorkerModel> getAll() {
		var workers = entityRepository.getAllEntities(WorkerModel.class);

		updateCachedWorkers(workers, false);

		return workers;
	}

	public Boolean getWorkerOnlineStatus(Long workerId) {
		return cachedWorkers.get(workerId).getRight();
	}

	private static synchronized void updateCachedWorkers(List<WorkerModel> workers, boolean removeList) {

		if (removeList) {
			for (var worker: workers) {
				cachedWorkers.remove(worker.id);
			}
		} else {
			for (var worker: workers) {
				if (!cachedWorkers.containsKey(worker.id)) {
					cachedWorkers.put(worker.id, new MutablePair<>(worker.address, false));
				} else {
					var oldPair = cachedWorkers.get(worker.id);
					cachedWorkers.replace(worker.id, new MutablePair<>(worker.address, oldPair.getRight()));
				}
			}
		}
	}

	private static Runnable syncWorkerStatisticsWithDB() {
		return () -> {

			for(Map.Entry<Long, Pair<String, Boolean>> entry : cachedWorkers.entrySet()) {
				Long id = entry.getKey();
				String address = entry.getValue().getLeft();

				var stats = StatisticsService.getWorkerStatistics(id, address);

				if (stats != null) {

					cachedWorkers.get(id).setValue(true);

					if (stats.utilization != null) {
						entityRepository.createEntity(stats.utilization.get(0));
					}
					if (stats.traffic != null) {
						for (var trafficEntity: stats.traffic) {
							if (trafficEntity.coordinatorId > 0) {
								entityRepository.createEntity(trafficEntity);
							}
						}
					}
					if (stats.events != null) {
						for (var eventEntity: stats.events) {
							if (eventEntity.coordinatorId > 0) {
								var eventId = entityRepository.createEntity(eventEntity);

								for (var stageEntity: eventEntity.stages) {
									stageEntity.eventId = eventId;

									entityRepository.createEntity(stageEntity);
								}
							}
						}
					}
					if (stats.dataObjects != null) {
						entityRepository.removeAllEntitiesByField(Constants.ENTITY_WORKER_ID_COL, id, DataObjectModel.class);

						for (var dataObjectEntity: stats.dataObjects) {
							entityRepository.createEntity(dataObjectEntity);
						}
					}
					if (stats.requests != null) {
						entityRepository.removeAllEntitiesByField(Constants.ENTITY_WORKER_ID_COL, id, RequestModel.class);

						for (var requestEntity: stats.requests) {
							if (requestEntity.coordinatorId > 0) {
								entityRepository.createEntity(requestEntity);
							}
						}
					}
				} else {
					cachedWorkers.get(id).setValue(false);
				}
			}
		};
	}
}
