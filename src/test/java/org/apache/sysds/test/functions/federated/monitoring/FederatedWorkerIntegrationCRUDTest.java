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

package org.apache.sysds.test.functions.federated.monitoring;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.WorkerModel;
import org.apache.sysds.test.TestConfiguration;
import org.apache.sysds.test.TestUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FederatedWorkerIntegrationCRUDTest extends FederatedMonitoringTestBase {
	private final static String TEST_NAME = "FederatedWorkerIntegrationCRUDTest";

	private final static String TEST_DIR = "functions/federated/monitoring/";
	private static final String TEST_CLASS_DIR = TEST_DIR + FederatedWorkerIntegrationCRUDTest.class.getSimpleName() + "/";

	@Override
	public void setUp() {
		TestUtils.clearAssertionInformation();
		addTestConfiguration(TEST_NAME, new TestConfiguration(TEST_CLASS_DIR, TEST_NAME, new String[] {"S"}));
		startFedMonitoring(null);
	}

	@Test
	public void testWorkerAddedForMonitoring() {
		var addedWorkers = addEntities(1, EntityType.WORKER);
		var firstWorkerStatus = addedWorkers.get(0).statusCode();

		Assert.assertEquals("Added worker status code", HttpStatus.SC_OK, firstWorkerStatus);
	}

	@Test
	@Ignore
	public void testWorkerRemovedFromMonitoring() {
		addEntities(2, EntityType.WORKER);
		var statusCode = removeEntity(1L, EntityType.WORKER).statusCode();

		var getAllWorkersResponse = getEntities(EntityType.WORKER);
		var numReturnedWorkers = StringUtils.countMatches(getAllWorkersResponse.body().toString(), "id");

		Assert.assertEquals("Removed worker status code", HttpStatus.SC_OK, statusCode);
		Assert.assertEquals("Removed workers num", 1, numReturnedWorkers);
	}

	@Test
	@Ignore
	public void testWorkerDataUpdated() {
		addEntities(3, EntityType.WORKER);
		var newWorkerData = new WorkerModel(1L, "NonExistentName", "nonexistent.address");

		var editedWorker = updateEntity(newWorkerData, EntityType.WORKER);

		var getAllWorkersResponse = getEntities(EntityType.WORKER);
		var numWorkersNewData = StringUtils.countMatches(getAllWorkersResponse.body().toString(), newWorkerData.name);

		Assert.assertEquals("Updated worker status code", HttpStatus.SC_OK, editedWorker.statusCode());
		Assert.assertEquals("Updated workers num", 1, numWorkersNewData);
	}

	@Test
	@Ignore
	public void testCorrectAmountAddedWorkersForMonitoring() {
		int numWorkers = 3;
		var addedWorkers = addEntities(numWorkers, EntityType.WORKER);

		for (int i = 0; i < numWorkers; i++) {
			var workerStatus = addedWorkers.get(i).statusCode();
			Assert.assertEquals("Added worker status code", HttpStatus.SC_OK, workerStatus);
		}

		var getAllWorkersResponse = getEntities(EntityType.WORKER);
		var numReturnedWorkers = StringUtils.countMatches(getAllWorkersResponse.body().toString(), "id");

		Assert.assertEquals("Amount of workers to get", numWorkers, numReturnedWorkers);
	}

	@Test
	@Ignore
	public void testWorkersReturnStatistics() {
		int numWorkers = 3;
		addEntities(numWorkers, EntityType.WORKER);

		for (int i = 0; i < numWorkers; i++) {
			var stats = getEntity(i, EntityType.STATISTICS);
			Assert.assertEquals("Received stats status code", HttpStatus.SC_OK, stats.statusCode());

			var body = stats.body().toString();

			Assert.assertTrue("Received stats content", body.contains("utilization"));
			Assert.assertTrue("Received stats content", body.contains("traffic"));
			Assert.assertTrue("Received stats content", body.contains("requests"));
			Assert.assertTrue("Received stats content", body.contains("events"));
			Assert.assertTrue("Received stats content", body.contains("dataObjects"));
		}
	}
}
