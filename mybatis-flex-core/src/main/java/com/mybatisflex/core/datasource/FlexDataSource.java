/**
 * Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mybatisflex.core.datasource;

import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.dialect.DbTypeUtil;
import com.mybatisflex.core.transaction.TransactionContext;
import com.mybatisflex.core.transaction.TransactionalManager;
import com.mybatisflex.core.util.StringUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FlexDataSource extends AbstractDataSource {

    private final Map<String, DataSource> dataSourceMap = new HashMap<>();
    private final Map<String, DbType> dbTypeHashMap = new HashMap<>();

    private final String defaultDataSourceKey;
    private final DataSource defaultDataSource;

    public FlexDataSource(String dataSourceKey, DataSource dataSource) {
        this.defaultDataSourceKey = dataSourceKey;
        this.defaultDataSource = dataSource;
        dataSourceMap.put(dataSourceKey, dataSource);
        dbTypeHashMap.put(dataSourceKey, DbTypeUtil.getDbType(dataSource));
    }

    public void addDataSource(String dataSourceKey, DataSource dataSource) {
        dataSourceMap.put(dataSourceKey, dataSource);
        dbTypeHashMap.put(dataSourceKey, DbTypeUtil.getDbType(dataSource));
    }

    public DbType getDbType(String dataSourceKey) {
        return dbTypeHashMap.get(dataSourceKey);
    }

    @Override
    public Connection getConnection() throws SQLException {
        String xid = TransactionContext.getXID();
        if (StringUtil.isNotBlank(xid)) {
            String dataSourceKey = DataSourceKey.get();
            if (StringUtil.isBlank(dataSourceKey)) {
                dataSourceKey = defaultDataSourceKey;
            }

            Connection connection = TransactionalManager.getConnection(xid, dataSourceKey);
            if (connection != null) {
                return connection;
            } else {
                connection = getDataSource().getConnection();
                TransactionalManager.hold(xid, dataSourceKey, connection);
                return connection;
            }
        } else {
            return getDataSource().getConnection();
        }
    }


    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        String xid = TransactionContext.getXID();
        if (StringUtil.isNotBlank(xid)) {
            String dataSourceKey = DataSourceKey.get();
            if (StringUtil.isBlank(dataSourceKey)) {
                dataSourceKey = defaultDataSourceKey;
            }
            Connection connection = TransactionalManager.getConnection(xid, dataSourceKey);
            if (connection != null) {
                return connection;
            } else {
                connection = getDataSource().getConnection(username, password);
                TransactionalManager.hold(xid, dataSourceKey, connection);
                return connection;
            }
        } else {
            return getDataSource().getConnection(username, password);
        }
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return getDataSource().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return (iface.isInstance(this) || getDataSource().isWrapperFor(iface));
    }


    private DataSource getDataSource() {
        DataSource dataSource = defaultDataSource;
        if (dataSourceMap.size() > 1) {
            String dataSourceKey = DataSourceKey.get();
            if (StringUtil.isNotBlank(dataSourceKey)) {
                dataSource = dataSourceMap.get(dataSourceKey);
                if (dataSource == null) {
                    throw new IllegalStateException("Cannot get target DataSource for dataSourceKey [" + dataSourceKey + "]");
                }
            }
        }
        return dataSource;
    }


}