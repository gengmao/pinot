/**
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
package org.apache.pinot.core.query.reduce;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.pinot.common.response.broker.BrokerResponseNative;
import org.apache.pinot.common.response.broker.ResultTable;
import org.apache.pinot.common.utils.DataSchema;
import org.apache.pinot.common.utils.DataTable;
import org.apache.pinot.core.query.request.context.QueryContext;
import org.apache.pinot.core.query.selection.SelectionOperatorUtils;
import org.apache.pinot.core.transport.ServerRoutingInstance;
import org.apache.pinot.core.util.QueryOptionsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SelectionOnlyStreamingReducer implements StreamingReducer {
  private static final Logger LOGGER = LoggerFactory.getLogger(SelectionOnlyStreamingReducer.class);

  private final QueryContext _queryContext;
  private final boolean _preserveType;
  private final int _limit;

  private DataSchema _dataSchema;
  private DataTableReducerContext _dataTableReducerContext;
  private List<Object[]> _rows;

  public SelectionOnlyStreamingReducer(QueryContext queryContext) {
    _queryContext = queryContext;
    _limit = _queryContext.getLimit();
    Map<String, String> queryOptions = queryContext.getQueryOptions();
    Preconditions.checkState(QueryOptionsUtils.isResponseFormatSQL(queryOptions), "only SQL response is supported");

    _preserveType = QueryOptionsUtils.isPreserveType(queryOptions);
    _dataSchema = null;
  }

  @Override
  public void init(DataTableReducerContext dataTableReducerContext) {
    _dataTableReducerContext = dataTableReducerContext;
    _rows = new ArrayList<>(Math.min(_limit, SelectionOperatorUtils.MAX_ROW_HOLDER_INITIAL_CAPACITY));
  }

  @Override
  public synchronized void reduce(ServerRoutingInstance key, DataTable dataTable) {
    // get dataSchema
    _dataSchema = _dataSchema == null ? dataTable.getDataSchema() : _dataSchema;
    // TODO: For data table map with more than one data tables, remove conflicting data tables
    reduceWithoutOrdering(dataTable, _limit);
  }

  private void reduceWithoutOrdering(DataTable dataTable, int limit) {
    int numRows = dataTable.getNumberOfRows();
    for (int rowId = 0; rowId < numRows; rowId++) {
      if (_rows.size() < limit) {
        _rows.add(SelectionOperatorUtils.extractRowFromDataTable(dataTable, rowId));
      } else {
        break;
      }
    }
  }

  @Override
  public BrokerResponseNative seal() {
    BrokerResponseNative brokerResponseNative = new BrokerResponseNative();
    List<String> selectionColumns = SelectionOperatorUtils.getSelectionColumns(_queryContext, _dataSchema);
    if (_dataSchema != null && _rows.size() > 0) {
      brokerResponseNative.setResultTable(
          SelectionOperatorUtils.renderResultTableWithoutOrdering(_rows, _dataSchema, selectionColumns));
    } else {
      // For empty data table map, construct empty result using the cached data schema for selection query
      DataSchema selectionDataSchema = SelectionOperatorUtils.getResultTableDataSchema(_dataSchema, selectionColumns);
      brokerResponseNative.setResultTable(new ResultTable(selectionDataSchema, Collections.emptyList()));
    }
    return brokerResponseNative;
  }
}
