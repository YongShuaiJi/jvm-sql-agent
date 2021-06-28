package com.alibaba.jvm.sandbox.module;

import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public class SqlUtil {

    private Object configuration;

    private Object boundSql;

    public SqlUtil(Object configuration, Object boundSql ){
        this.configuration = configuration;
        this.boundSql = boundSql;
    }

    public String getSql() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class boundSqlClass = boundSql.getClass();
        // String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        Method field_sql = boundSqlClass.getMethod("getSql");
        String sql = String.valueOf(field_sql.invoke(boundSql)).replaceAll("[\\s]+", " ");

        // Object parameterObject = boundSql.getParameterObject();
        Method ParameterObjectMethod = boundSqlClass.getMethod("getParameterObject"); // public
        Object parameterObject = ParameterObjectMethod.invoke(boundSql); // 参数对象

        // List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        Method ParameterMappingsMethod = boundSqlClass.getMethod("getParameterMappings"); // public
        Object ParameterMappingsObject = ParameterMappingsMethod.invoke(boundSql);

        List parameterMappings = new ArrayList();  // 有序参数映射列表
        if (ParameterMappingsObject instanceof ArrayList) {
            for (Object o : (List<?>) ParameterMappingsObject) {
                parameterMappings.add(o);
            }
        }

        if (CollectionUtils.isNotEmpty(parameterMappings) && parameterObject != null) {
            // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换
            // TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();

            Method TypeHandlerRegistryMethod = configuration.getClass().getMethod("getTypeHandlerRegistry");
            Object typeHandlerRegistry = TypeHandlerRegistryMethod.invoke(configuration);

            Method hasTypeHandlerMethod = typeHandlerRegistry.getClass().getMethod("hasTypeHandler", Class.class);
            boolean boll = (boolean) hasTypeHandlerMethod.invoke(typeHandlerRegistry, parameterObject.getClass());

            // 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
            if (boll) {
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                // MetaObject主要是封装了originalObject对象，提供了get和set的方法用于获取和设置originalObject的属性值,主要支持对JavaBean、Collection、Map三种类型对象的操作
//                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                Method newMetaObjectMethod = configuration.getClass().getMethod("newMetaObject",Object.class);
                Object metaObject = newMetaObjectMethod.invoke(configuration,parameterObject);

                for (Object parameterMapping : parameterMappings) {
                    // String propertyName = parameterMapping.getProperty();
                    Method getPropertyMethod = parameterMapping.getClass().getMethod("getProperty");
                    String propertyName = String.valueOf(getPropertyMethod.invoke(parameterMapping));

                    // metaObject.hasGetter(propertyName)
                    Method hasGetterMethod = metaObject.getClass().getMethod("hasGetter",String.class);
                    boolean var1 = (boolean)hasGetterMethod.invoke(metaObject,propertyName);

                    Method hasAdditionalParameterMethod = boundSqlClass.getMethod("hasAdditionalParameter",String.class);
                    boolean var3 =  (boolean)hasAdditionalParameterMethod.invoke(boundSql,propertyName);

                    if (var1) {
                        //  Object obj = metaObject.getValue(propertyName);
                        Method getValueMethod = metaObject.getClass().getMethod("getValue",String.class);
                        Object var2 = getValueMethod.invoke(metaObject,propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(var2)));
                    } else if (var3) {
                        // 该分支是动态sql
                        // Object obj = boundSql.getAdditionalParameter(propertyName);
                        Method getAdditionalParameterMethod = boundSqlClass.getMethod("getAdditionalParameter",String.class);
                        Object var4 =  getAdditionalParameterMethod.invoke(boundSql,propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(var4)));
                    } else {
                        // 打印出缺失，提醒该参数缺失并防止错位
                        sql = sql.replaceFirst("\\?", "缺失");
                    }
                }
            }
        }
        return sql;
    }

    private String getParameterValue(Object obj) {
        String value = null;
        if (obj instanceof String) {
            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {
            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(new Date()) + "'";
        } else {
            if (obj != null) {
                value = obj.toString();
            } else {
                value = "";
            }

        }
        return value;
    }
}
