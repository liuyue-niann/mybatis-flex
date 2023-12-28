/*
 *  Copyright (c) 2022-2023, Mybatis-Flex (fuhai999@gmail.com).
 *  <p>
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.mybatisflex.test.mapper;

import com.mybatisflex.core.field.QueryBuilder;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.test.model.Gender;
import com.mybatisflex.test.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;

import static com.mybatisflex.test.model.table.RoleTableDef.ROLE;
import static com.mybatisflex.test.model.table.UserRoleTableDef.USER_ROLE;
import static com.mybatisflex.test.model.table.UserTableDef.USER;

/**
 * @author 王帅
 * @since 2023-08-08
 */
@SpringBootTest
class QueryChainTest {

    static final String TEST_DDL = QueryChainTest.class.getClassLoader().getResource("flex_test.sql").getPath();
    static GenericContainer<?> mysql = new GenericContainer<>(DockerImageName.parse("mysql:8.2.0"))
        .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 1))
        // bind DDL to container /docker-entrypoint-initdb.d and it will execute automatically
        .withFileSystemBind(TEST_DDL, "/docker-entrypoint-initdb.d/flex_test.sql", BindMode.READ_ONLY)
        .withEnv("MYSQL_DATABASE", "flex_test")
        .withEnv("MYSQL_ROOT_PASSWORD", "123456");

    @Autowired
    UserMapper userMapper;

    @BeforeAll
    public static void start() {
        mysql.setPortBindings(Collections.singletonList("3306:3306"));
        mysql.start();
    }

    @AfterAll
    public static void stop() {
        mysql.stop();
    }

    @Test
    void testFields() {
        QueryBuilder<User> builder = user ->
            QueryWrapper.create()
                .select()
                .from(ROLE)
                .where(ROLE.ROLE_ID.in(
                    QueryWrapper.create()
                        .select(USER_ROLE.ROLE_ID)
                        .from(USER_ROLE)
                        .where(USER_ROLE.USER_ID.eq(user.getUserId()))
                ));

        User user1 = QueryChain.of(userMapper)
            .where(USER.USER_ID.eq(1))
            .withFields()
            .fieldMapping(User::getRoleList, builder)
            .one();

        User user2 = User.create()
            .where(USER.USER_ID.eq(1))
            .withFields()
            .fieldMapping(User::getRoleList, builder)
            .one();

        Assertions.assertEquals(user1.toString(), user2.toString());
    }

    @Test
    void testRelations() {
        User user1 = QueryChain.of(userMapper)
            .where(USER.USER_ID.eq(2))
            .withRelations()
            .one();

        User user2 = User.create()
            .where(USER.USER_ID.eq(2))
            .withRelations()
            .one();

        Assertions.assertEquals(user1.toString(), user2.toString());
    }

    @Test
    void testToSql() {
        String sql = UpdateChain.create(userMapper)
            .set(USER.USER_NAME, "张三")
            .set(User::getUserId, Gender.MALE)
            .where(USER.USER_ID.eq(1))
            .toSQL();

        System.out.println(sql);
    }

}
