/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.federation.executor.original.table;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.rel2sql.RelToSqlConverter;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.util.SqlString;
import org.apache.calcite.tools.RelBuilder;
import org.apache.shardingsphere.infra.binder.LogicSQL;
import org.apache.shardingsphere.infra.binder.SQLStatementContextFactory;
import org.apache.shardingsphere.infra.binder.statement.SQLStatementContext;
import org.apache.shardingsphere.infra.context.kernel.KernelProcessor;
import org.apache.shardingsphere.infra.database.type.DatabaseType;
import org.apache.shardingsphere.infra.database.type.DatabaseTypeEngine;
import org.apache.shardingsphere.infra.exception.ShardingSphereException;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroup;
import org.apache.shardingsphere.infra.executor.kernel.model.ExecutionGroupContext;
import org.apache.shardingsphere.infra.executor.sql.context.ExecutionContext;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutionUnit;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutor;
import org.apache.shardingsphere.infra.executor.sql.execute.engine.driver.jdbc.JDBCExecutorCallback;
import org.apache.shardingsphere.infra.executor.sql.execute.result.ExecuteResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.QueryResultMetaData;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.driver.jdbc.type.memory.JDBCMemoryQueryResult;
import org.apache.shardingsphere.infra.executor.sql.execute.result.query.impl.driver.jdbc.type.stream.JDBCStreamQueryResult;
import org.apache.shardingsphere.infra.executor.sql.prepare.driver.DriverExecutionPrepareEngine;
import org.apache.shardingsphere.infra.executor.sql.process.ExecuteProcessEngine;
import org.apache.shardingsphere.infra.federation.executor.FederationContext;
import org.apache.shardingsphere.infra.federation.executor.original.SQLDialectFactory;
import org.apache.shardingsphere.infra.federation.executor.original.row.EmptyRowEnumerator;
import org.apache.shardingsphere.infra.federation.executor.original.row.FilterableRowEnumerator;
import org.apache.shardingsphere.infra.federation.optimizer.context.OptimizerContext;
import org.apache.shardingsphere.infra.federation.optimizer.context.planner.OptimizerPlannerContextFactory;
import org.apache.shardingsphere.infra.federation.optimizer.executor.TableScanExecutorContext;
import org.apache.shardingsphere.infra.federation.optimizer.executor.TableScanExecutor;
import org.apache.shardingsphere.infra.federation.optimizer.metadata.filter.FilterableSchema;
import org.apache.shardingsphere.infra.federation.optimizer.planner.QueryOptimizePlannerFactory;
import org.apache.shardingsphere.infra.merge.MergeEngine;
import org.apache.shardingsphere.infra.merge.result.MergedResult;
import org.apache.shardingsphere.infra.metadata.database.ShardingSphereDatabase;
import org.apache.shardingsphere.infra.metadata.database.rule.ShardingSphereRuleMetaData;
import org.apache.shardingsphere.infra.metadata.database.schema.decorator.model.ShardingSphereSchema;
import org.apache.shardingsphere.infra.metadata.database.schema.decorator.model.ShardingSphereTable;
import org.apache.shardingsphere.infra.parser.sql.SQLStatementParserEngine;
import org.apache.shardingsphere.infra.util.eventbus.EventBusContext;
import org.apache.shardingsphere.sql.parser.sql.common.statement.SQLStatement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filterable table scan executor.
 */
@RequiredArgsConstructor
public final class FilterableTableScanExecutor implements TableScanExecutor {
    
    private final DriverExecutionPrepareEngine<JDBCExecutionUnit, Connection> prepareEngine;
    
    private final JDBCExecutor jdbcExecutor;
    
    private final JDBCExecutorCallback<? extends ExecuteResult> callback;
    
    private final OptimizerContext optimizerContext;
    
    private final ShardingSphereRuleMetaData globalRuleMetaData;
    
    private final FilterableTableScanExecutorContext executorContext;
    
    private final EventBusContext eventBusContext;
    
    @Override
    public Enumerable<Object[]> execute(final ShardingSphereTable table, final TableScanExecutorContext scanContext) {
        String databaseName = executorContext.getDatabaseName();
        String schemaName = executorContext.getSchemaName();
        DatabaseType databaseType = DatabaseTypeEngine.getTrunkDatabaseType(optimizerContext.getParserContexts().get(databaseName).getDatabaseType().getType());
        SqlString sqlString = createSQLString(table, scanContext, SQLDialectFactory.getSQLDialect(databaseType));
        // TODO replace sql parse with sql convert
        FederationContext federationContext = executorContext.getFederationContext();
        LogicSQL logicSQL = createLogicSQL(federationContext.getDatabases(), sqlString, databaseType);
        ShardingSphereDatabase database = federationContext.getDatabases().get(databaseName.toLowerCase());
        ExecutionContext context = new KernelProcessor().generateExecutionContext(logicSQL, database, globalRuleMetaData, executorContext.getProps());
        if (federationContext.isPreview() || databaseType.getSystemSchemas().contains(schemaName)) {
            federationContext.getExecutionUnits().addAll(context.getExecutionUnits());
            return createEmptyEnumerable();
        }
        return execute(databaseType, logicSQL, database, context);
    }
    
    private AbstractEnumerable<Object[]> execute(final DatabaseType databaseType, final LogicSQL logicSQL, final ShardingSphereDatabase database, final ExecutionContext context) {
        try {
            ExecutionGroupContext<JDBCExecutionUnit> executionGroupContext = prepareEngine.prepare(context.getRouteContext(), context.getExecutionUnits());
            setParameters(executionGroupContext.getInputGroups());
            ExecuteProcessEngine.initialize(context.getLogicSQL(), executionGroupContext, eventBusContext);
            List<QueryResult> queryResults = execute(executionGroupContext, databaseType);
            ExecuteProcessEngine.finish(executionGroupContext.getExecutionID(), eventBusContext);
            MergeEngine mergeEngine = new MergeEngine(database, executorContext.getProps());
            MergedResult mergedResult = mergeEngine.merge(queryResults, logicSQL.getSqlStatementContext());
            Collection<Statement> statements = getStatements(executionGroupContext.getInputGroups());
            return createEnumerable(mergedResult, queryResults.get(0).getMetaData(), statements);
        } catch (final SQLException ex) {
            throw new ShardingSphereException(ex);
        } finally {
            ExecuteProcessEngine.clean();
        }
    }
    
    private List<QueryResult> execute(final ExecutionGroupContext<JDBCExecutionUnit> executionGroupContext, final DatabaseType databaseType) throws SQLException {
        Collection<QueryResult> queryResults = jdbcExecutor.execute(executionGroupContext, callback).stream().map(each -> (QueryResult) each).collect(Collectors.toList());
        List<QueryResult> result = new LinkedList<>();
        for (QueryResult each : queryResults) {
            QueryResult queryResult = each instanceof JDBCStreamQueryResult
                    ? new JDBCMemoryQueryResult(((JDBCStreamQueryResult) each).getResultSet(), databaseType)
                    : each;
            result.add(queryResult);
        }
        return result;
    }
    
    private Collection<Statement> getStatements(final Collection<ExecutionGroup<JDBCExecutionUnit>> inputGroups) {
        Collection<Statement> result = new LinkedList<>();
        for (ExecutionGroup<JDBCExecutionUnit> each : inputGroups) {
            for (JDBCExecutionUnit executionUnit : each.getInputs()) {
                result.add(executionUnit.getStorageResource());
            }
        }
        return result;
    }
    
    private SqlString createSQLString(final ShardingSphereTable table, final TableScanExecutorContext scanContext, final SqlDialect sqlDialect) {
        return new RelToSqlConverter(sqlDialect).visitRoot(createRelNode(table, scanContext)).asStatement().toSqlString(sqlDialect);
    }
    
    private void setParameters(final Collection<ExecutionGroup<JDBCExecutionUnit>> inputGroups) {
        for (ExecutionGroup<JDBCExecutionUnit> each : inputGroups) {
            for (JDBCExecutionUnit executionUnit : each.getInputs()) {
                if (!(executionUnit.getStorageResource() instanceof PreparedStatement)) {
                    continue;
                }
                setParameters((PreparedStatement) executionUnit.getStorageResource(), executionUnit.getExecutionUnit().getSqlUnit().getParameters());
            }
        }
    }
    
    @SneakyThrows(SQLException.class)
    private void setParameters(final PreparedStatement preparedStatement, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            Object parameter = parameters.get(i);
            preparedStatement.setObject(i + 1, parameter);
        }
    }
    
    private RelNode createRelNode(final ShardingSphereTable table, final TableScanExecutorContext scanContext) {
        String databaseName = executorContext.getDatabaseName();
        String schemaName = executorContext.getSchemaName();
        CalciteConnectionConfig connectionConfig = new CalciteConnectionConfigImpl(OptimizerPlannerContextFactory.createConnectionProperties());
        ShardingSphereSchema schema = executorContext.getFederationContext().getDatabases().get(databaseName).getSchema(schemaName);
        CalciteCatalogReader catalogReader = OptimizerPlannerContextFactory.createCatalogReader(schemaName,
                new FilterableSchema(schemaName, schema, null), new JavaTypeFactoryImpl(), connectionConfig);
        RelOptCluster relOptCluster = RelOptCluster.create(QueryOptimizePlannerFactory.createVolcanoPlanner(), new RexBuilder(new JavaTypeFactoryImpl()));
        RelBuilder builder = RelFactories.LOGICAL_BUILDER.create(relOptCluster, catalogReader).scan(table.getName()).filter(scanContext.getFilters());
        if (null != scanContext.getProjects()) {
            builder.project(createProjections(scanContext.getProjects(), builder, table.getColumnNames()));
        }
        return builder.build();
    }
    
    private Collection<RexNode> createProjections(final int[] projects, final RelBuilder relBuilder, final List<String> columnNames) {
        Collection<RexNode> result = new LinkedList<>();
        for (int each : projects) {
            result.add(relBuilder.field(columnNames.get(each)));
        }
        return result;
    }
    
    private AbstractEnumerable<Object[]> createEnumerable(final MergedResult mergedResult, final QueryResultMetaData metaData, final Collection<Statement> statements) {
        return new AbstractEnumerable<Object[]>() {
            
            @Override
            public Enumerator<Object[]> enumerator() {
                return new FilterableRowEnumerator(mergedResult, metaData, statements);
            }
        };
    }
    
    private LogicSQL createLogicSQL(final Map<String, ShardingSphereDatabase> databases, final SqlString sqlString, final DatabaseType databaseType) {
        String sql = sqlString.getSql().replace("\n", " ");
        SQLStatement sqlStatement = new SQLStatementParserEngine(databaseType.getType(),
                optimizerContext.getSqlParserRule().getSqlStatementCache(), optimizerContext.getSqlParserRule().getParseTreeCache(),
                optimizerContext.getSqlParserRule().isSqlCommentParseEnabled()).parse(sql, false);
        List<Object> parameters = getParameters(sqlString.getDynamicParameters());
        SQLStatementContext<?> sqlStatementContext = SQLStatementContextFactory.newInstance(databases, parameters, sqlStatement, executorContext.getDatabaseName());
        return new LogicSQL(sqlStatementContext, sql, parameters);
    }
    
    private List<Object> getParameters(final List<Integer> parameterIndexes) {
        if (null == parameterIndexes) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<>();
        for (Integer each : parameterIndexes) {
            result.add(executorContext.getFederationContext().getLogicSQL().getParameters().get(each));
        }
        return result;
    }
    
    private AbstractEnumerable<Object[]> createEmptyEnumerable() {
        return new AbstractEnumerable<Object[]>() {
            
            @Override
            public Enumerator<Object[]> enumerator() {
                return new EmptyRowEnumerator();
            }
        };
    }
}
