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

package org.apache.sysds.runtime.controlprogram.federated.monitoring.repositories;

import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.BaseModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.CoordinatorModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.EventModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.EventStageModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.TrafficModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.UtilizationModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.models.WorkerModel;
import org.apache.sysds.runtime.controlprogram.federated.monitoring.services.MapperService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DerbyRepository implements IRepository {
	private final static String DB_CONNECTION = "jdbc:derby:memory:derbyDB";
	private final Connection _db;
	private final List<BaseModel> _allEntities = new ArrayList<>(List.of(
			new WorkerModel(),
			new CoordinatorModel(),
			new UtilizationModel(),
			new TrafficModel(),
			new EventModel(),
			new EventStageModel()
	));
	private static final String ENTITY_SCHEMA_CREATE_STMT = "CREATE TABLE %s " +
			"(id INTEGER PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)";
	private static final String ENTITY_INSERT_STMT = "INSERT INTO %s VALUES %s";
	private static final String GET_ENTITY_WITH_COL_STMT = "SELECT * FROM %s WHERE %s = ?";
	private static final String GET_ENTITY_WITH_COL_LIMIT_STMT = "SELECT * FROM %s " +
			"WHERE %s = ? " +
			"ORDER BY ID DESC " +
			"FETCH FIRST %d ROWS ONLY";
	private static final String DELETE_ENTITY_WITH_COL_STMT = "DELETE FROM %s WHERE %s = ?";
	private static final String UPDATE_ENTITY_WITH_COL_STMT = "UPDATE %s SET %s = ?, %s = ? WHERE %s = ?";
	private static final String GET_ALL_ENTITIES_STMT = "SELECT * FROM %s";

	public DerbyRepository() {
		_db = createMonitoringDatabase();
	}

	private Connection createMonitoringDatabase() {
		Connection db = null;
		try {
			// Creates only if DB doesn't exist
			db = DriverManager.getConnection(DB_CONNECTION + ";create=true");
			createMonitoringEntitiesInDB(db);

			return db;
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private void createMonitoringEntitiesInDB(Connection db) {
		try {
			var dbMetaData = db.getMetaData();

			for (var entity: _allEntities) {
				var entityName = entity.getClass().getSimpleName().replace(Constants.ENTITY_CLASS_SUFFIX, "");
				var entityExist = dbMetaData.getTables(null, null, entityName.toUpperCase(),null);

				if(!entityExist.next()) {
					StringBuilder sb = new StringBuilder();

					sb.append(String.format(ENTITY_SCHEMA_CREATE_STMT, entityName));

					var fields = entity.getClass().getFields();
					for (var field: fields) {

						if (field.getName().equalsIgnoreCase(Constants.ENTITY_ID_COL)) {
							continue;
						}

						if (field.getType().isAssignableFrom(String.class)) {
							sb.append(String.format(",%s %s", field.getName(), Constants.ENTITY_STRING_COL));
						} else if (field.getType().isAssignableFrom(double.class)) {
							sb.append(String.format(",%s %s", field.getName(), Constants.ENTITY_DOUBLE_COL));
						} else if (field.getType().isAssignableFrom(Long.class) ||
								field.getType().isAssignableFrom(int.class)) {
							sb.append(String.format(",%s %s", field.getName(), Constants.ENTITY_NUMBER_COL));
						} else if (field.getType().isAssignableFrom(LocalDateTime.class)) {
							sb.append(String.format(",%s %s", field.getName(), Constants.ENTITY_TIMESTAMP_COL));
						}

					}

					sb.append(")");

					PreparedStatement st = db.prepareStatement(sb.toString());
					st.executeUpdate();
				}
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public <T extends BaseModel> Long createEntity(T model) {

		PreparedStatement st = null;
		long id = -1L;

		try {

			StringBuilder sb = new StringBuilder();

			var entityName = model.getClass().getSimpleName().replace(Constants.ENTITY_CLASS_SUFFIX, "");

			sb.append(String.format("%s (", entityName));

			var fields = model.getClass().getFields();
			int dbFieldCount = 0;
			for (var field: fields) {

				if (field.getName().equalsIgnoreCase(Constants.ENTITY_ID_COL)) {
					continue;
				}

				if (field.getType().isAssignableFrom(String.class) ||
					field.getType().isAssignableFrom(double.class) ||
					field.getType().isAssignableFrom(Long.class) ||
					field.getType().isAssignableFrom(int.class) ||
					field.getType().isAssignableFrom(LocalDateTime.class)) {
					sb.append(String.format("%s,", field.getName()));
					dbFieldCount++;
				}
			}

			sb.replace(sb.length() - 1, sb.length(), ")");
			String bindVarsStr = String.format("(%s)", String.join(",", Collections.nCopies(dbFieldCount, "?")));

			st = _db.prepareStatement(String.format(ENTITY_INSERT_STMT, sb, bindVarsStr), PreparedStatement.RETURN_GENERATED_KEYS);

			int bindVarIndex = 1;
			for (var field: fields) {

				if (field.getName().equalsIgnoreCase(Constants.ENTITY_ID_COL)) {
					continue;
				}

				if (field.getType().isAssignableFrom(String.class)) {
					st.setString(bindVarIndex, String.valueOf(field.get(model)));
					bindVarIndex++;
				} else if (field.getType().isAssignableFrom(double.class)) {
					st.setDouble(bindVarIndex, (double) field.get(model));
					bindVarIndex++;
				} else if (field.getType().isAssignableFrom(Long.class) ||
						field.getType().isAssignableFrom(int.class)) {
					st.setLong(bindVarIndex, (long) field.get(model));
					bindVarIndex++;
				} else if (field.getType().isAssignableFrom(LocalDateTime.class)) {
					st.setTimestamp(bindVarIndex, Timestamp.valueOf((LocalDateTime) field.get(model)));
					bindVarIndex++;
				}
			}

			st.executeUpdate();

			ResultSet rs = st.getGeneratedKeys();
			if (rs.next()) {
				id = rs.getLong(1); // this is the auto-generated id key
			}

		} catch (SQLException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}

		return id;
	}

	public <T extends BaseModel> T getEntity(Long id, Class<T> type) {
		T resultModel = null;

		try {
			var entityName = type.getSimpleName().replace(Constants.ENTITY_CLASS_SUFFIX, "");

			PreparedStatement st = _db.prepareStatement(
				String.format(GET_ENTITY_WITH_COL_STMT, entityName, Constants.ENTITY_ID_COL));

			st.setLong(1, id);
			var resultSet = st.executeQuery();

			if (resultSet.next()){
				resultModel = MapperService.mapResultToModel(resultSet, type);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return resultModel;
	}

	public <T extends BaseModel> List<T> getAllEntities(Class<T> type) {
		List<T> resultModels = new ArrayList<>();

		try {
			var entityName = type.getSimpleName().replace(Constants.ENTITY_CLASS_SUFFIX, "");

			PreparedStatement st = _db.prepareStatement(
				String.format(GET_ALL_ENTITIES_STMT, entityName));

			var resultSet = st.executeQuery();
			while (resultSet.next()){
				resultModels.add(MapperService.mapResultToModel(resultSet, type));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return resultModels;
	}
	public <T extends BaseModel> List<T> getAllEntitiesByField(String fieldName, Object value, Class<T> type) {
		return getAllEntitiesByField(fieldName, value, type, -1);
	}

	public <T extends BaseModel> List<T> getAllEntitiesByField(String fieldName, Object value, Class<T> type, int rowCount) {
		List<T> resultModels = new ArrayList<>();
		PreparedStatement st = null;

		try {
			var entityName = type.getSimpleName().replace(Constants.ENTITY_CLASS_SUFFIX, "");

			if (rowCount < 0) {
				st = _db.prepareStatement(
						String.format(GET_ENTITY_WITH_COL_STMT, entityName, fieldName));
			} else {
				st = _db.prepareStatement(
						String.format(GET_ENTITY_WITH_COL_LIMIT_STMT, entityName, fieldName, rowCount));
			}

			if (value.getClass().isAssignableFrom(String.class)) {
				st.setString(1, String.valueOf(value));
			} else if (value.getClass().isAssignableFrom(Long.class)) {
				st.setLong(1, Long.parseLong(String.valueOf(value)));
			}

			var resultSet = st.executeQuery();
			while (resultSet.next()){
				resultModels.add(MapperService.mapResultToModel(resultSet, type));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return resultModels;
	}

	@Override
	public <T extends BaseModel> void updateEntity(T model) {

//		try {
//			PreparedStatement st = _db.prepareStatement(
//				String.format(UPDATE_ENTITY_WITH_COL_STMT, Constants.WORKERS_TABLE_NAME,
//					Constants.ENTITY_NAME_COL,
//					Constants.ENTITY_ADDR_COL,
//					Constants.ENTITY_ID_COL));
//			NodeEntityModel editModel = (NodeEntityModel) model;
//
//			if (type == EntityEnum.COORDINATOR) {
//				st = _db.prepareStatement(
//					String.format(UPDATE_ENTITY_WITH_COL_STMT, Constants.COORDINATORS_TABLE_NAME,
//						Constants.ENTITY_NAME_COL,
//						Constants.ENTITY_ADDR_COL,
//						Constants.ENTITY_ID_COL));
//			}
//
//			st.setString(1, editModel.getName());
//			st.setString(2, editModel.getAddress());
//			st.setLong(3, editModel.getId());
//
//			st.executeUpdate();
//
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
	}

	@Override
	public <T extends BaseModel> void removeEntity(Long id, Class<T> type) {
//		PreparedStatement st = null;
//		try {
//			if (type == EntityEnum.WORKER) {
//				st = _db.prepareStatement(
//					String.format(DELETE_ENTITY_WITH_COL_STMT, Constants.WORKERS_TABLE_NAME, Constants.ENTITY_ID_COL));
//				st.setLong(1, id);
//			} else {
//				st = _db.prepareStatement(
//					String.format(DELETE_ENTITY_WITH_COL_STMT, Constants.COORDINATORS_TABLE_NAME, Constants.ENTITY_ID_COL));
//				st.setLong(1, id);
//			}
//			st.executeUpdate();
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
	}
}
