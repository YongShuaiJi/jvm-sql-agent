package com.alibaba.jvm.sandbox.module;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.kohsuke.MetaInfServices;
import javax.annotation.Resource;
import java.lang.reflect.*;
import java.util.logging.Logger;

@MetaInfServices(Module.class)
@Information(id = "Mybatis-SQL-Intercepter", version = "0.1", author = "jysnana@163.com")
public class JDBCModule implements Module, LoadCompleted {

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private static Logger logger = Logger.getLogger(JDBCModule.class.getName());

    @Override
    public void loadCompleted() {
        SQLStatement();
    }

    private void SQLStatement(){
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("org.apache.ibatis.executor.Executor")
                .includeSubClasses() //包含所有实现类或子类
                .onBehavior("query") // 行为名称
                .withParameterTypes(MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class)
                .onWatch(new AdviceListener(){
                    @Override
                    protected void afterReturning(Advice advice) throws Throwable {
                        String behaviorName = advice.getBehavior().getName();
                        logger.info("BehaviorName = " + behaviorName);
                        Object[] args = advice.getParameterArray();
                        Object object = args[0];
                        Class clazz = object.getClass();
                        Object parameter = null;
                        if (advice.getParameterArray().length > 1) {
                            parameter = advice.getParameterArray()[1];
                        }
                        Method method = clazz.getMethod("getBoundSql",Object.class);
                        Object boundSqls = method.invoke(object,parameter); // BoundSql 对象
                        Method configurationMethod =  clazz.getMethod("getConfiguration");
                        Object configuration = configurationMethod.invoke(object); // Configuration 对象
                        SqlUtil sqlUtil = new SqlUtil(configuration,boundSqls);
                        String targetSQL = sqlUtil.getSql();
                        RabbitTemplate.getInstance().sendMessage(targetSQL);
                        logger.info("SQL = " + targetSQL);
                    }
                });
    }

}
