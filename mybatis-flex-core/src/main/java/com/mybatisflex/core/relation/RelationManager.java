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
package com.mybatisflex.core.relation;

import com.mybatisflex.annotation.RelationManyToMany;
import com.mybatisflex.annotation.RelationManyToOne;
import com.mybatisflex.annotation.RelationOneToMany;
import com.mybatisflex.annotation.RelationOneToOne;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;
import com.mybatisflex.core.util.ClassUtil;
import com.mybatisflex.core.util.CollectionUtil;
import com.mybatisflex.core.util.StringUtil;
import org.apache.ibatis.util.MapUtil;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.mybatisflex.core.query.QueryMethods.column;

/**
 * @author michael
 */
public class RelationManager {

    private RelationManager() {
    }

    private static Map<Class<?>, List<AbstractRelation>> classRelations = new ConcurrentHashMap<>();

    private static List<AbstractRelation> getRelations(Class<?> clazz) {
        return MapUtil.computeIfAbsent(classRelations, clazz, RelationManager::doGetRelations);
    }

    private static List<AbstractRelation> doGetRelations(Class<?> entityClass) {
        List<Field> allFields = ClassUtil.getAllFields(entityClass);
        List<AbstractRelation> relations = new ArrayList<>();
        for (Field field : allFields) {
            RelationManyToMany manyToManyAnnotation = field.getAnnotation(RelationManyToMany.class);
            if (manyToManyAnnotation != null) {
                relations.add(new ManyToMany<>(manyToManyAnnotation, entityClass, field));
            }

            RelationManyToOne manyToOneAnnotation = field.getAnnotation(RelationManyToOne.class);
            if (manyToOneAnnotation != null) {
                relations.add(new ManyToOne<>(manyToOneAnnotation, entityClass, field));
            }

            RelationOneToMany oneToManyAnnotation = field.getAnnotation(RelationOneToMany.class);
            if (oneToManyAnnotation != null) {
                relations.add(new OneToMany<>(oneToManyAnnotation, entityClass, field));
            }

            RelationOneToOne oneToOneAnnotation = field.getAnnotation(RelationOneToOne.class);
            if (oneToOneAnnotation != null) {
                relations.add(new OneToOne<>(oneToOneAnnotation, entityClass, field));
            }
        }
        return relations;
    }


    public static <Entity> void queryRelations(BaseMapper<?> mapper, List<Entity> entities) {
        doQueryRelations(mapper, entities, new HashSet<>());
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    static <Entity> void doQueryRelations(BaseMapper<?> mapper, List<Entity> entities, Set<Class<?>> queriedClasses) {
        if (CollectionUtil.isEmpty(entities)) {
            return;
        }
        Class<Entity> objectClass = (Class<Entity>) entities.get(0).getClass();
        if (queriedClasses.contains(objectClass)) {
            return;
        } else {
            queriedClasses.add(objectClass);
        }
        List<AbstractRelation> relations = getRelations(objectClass);
        if (relations.isEmpty()) {
            return;
        }
        String currentDsKey = DataSourceKey.get();
        try {
            relations.forEach(relation -> {

                Class mappingType = relation.getMappingType();
                if (queriedClasses.contains(mappingType)) {
                    return;
                }

                Set<Object> targetValues;
                List<Row> mappingRows = null;

                //通过中间表关联查询
                if (relation.isRelationByMiddleTable()) {
                    targetValues = new HashSet<>();
                    Set selfFieldValues = relation.getSelfFieldValues(entities);
                    QueryWrapper queryWrapper = QueryWrapper.create().select()
                        .from(relation.getJoinTable());
                    if (selfFieldValues.size() > 1) {
                        queryWrapper.where(column(relation.getJoinSelfColumn()).in(selfFieldValues));
                    } else {
                        queryWrapper.where(column(relation.getJoinTargetColumn()).eq(selfFieldValues.iterator().next()));
                    }

                    mappingRows = mapper.selectListByQueryAs(queryWrapper, Row.class);
                    if (CollectionUtil.isEmpty(mappingRows)) {
                        return;
                    }

                    for (Row mappingData : mappingRows) {
                        Object targetValue = mappingData.getIgnoreCase(relation.getJoinTargetColumn());
                        if (targetValue != null) {
                            targetValues.add(targetValue);
                        }
                    }
                }
                //通过外键字段关联查询
                else {
                    targetValues = relation.getSelfFieldValues(entities);
                }

                if (CollectionUtil.isEmpty(targetValues)) {
                    return;
                }


                String dataSource = relation.getDataSource();
                if (StringUtil.isBlank(dataSource) && currentDsKey != null) {
                    dataSource = currentDsKey;
                }

                try {
                    if (StringUtil.isNotBlank(dataSource)) {
                        DataSourceKey.use(dataSource);
                    }

                    QueryWrapper queryWrapper = relation.buildQueryWrapper(targetValues);
                    List<?> targetObjectList = mapper.selectListByQueryAs(queryWrapper, mappingType);
                    if (CollectionUtil.isNotEmpty(targetObjectList)) {
                        doQueryRelations(mapper, targetObjectList, queriedClasses);
                        relation.join(entities, targetObjectList, mappingRows);
                    }
                } finally {
                    if (StringUtil.isNotBlank(dataSource)) {
                        DataSourceKey.clear();
                    }
                }
            });
        } finally {
            if (currentDsKey != null) {
                DataSourceKey.use(currentDsKey);
            }
        }
    }
}