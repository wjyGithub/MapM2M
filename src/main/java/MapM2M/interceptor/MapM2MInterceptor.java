package MapM2M.interceptor;


import MapM2M.VALUE;
import MapM2M.exception.HapException;
import MapM2M.util.ReflectUtil;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import MapM2M.annotation.MapM2M;

/**
 * MapM2M拦截器
 * @author jianyuan.wei@hand-china.com
 * @date 2018/10/13 23:36
 */
@Intercepts(@Signature(method = "handleResultSets",type= ResultSetHandler.class,args = {Statement.class}))
public class MapM2MInterceptor implements Interceptor {

    private Logger logger = LoggerFactory.getLogger(MapM2MInterceptor.class);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MetaObject metaStatementHandler = ReflectUtil.getRealTarget(invocation);
        MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("mappedStatement");

        String className = StringUtils.substringBeforeLast(mappedStatement.getId(), ".");// 当前类
        String currentMethodName = StringUtils.substringAfterLast(mappedStatement.getId(), ".");// 当前方法
        Method currentMethod = findMethod(className, currentMethodName);// 获取当前Method

        if (currentMethod == null || currentMethod.getAnnotation(MapM2M.class) == null) {// 如果当前Method没有注解MapM2M
            return invocation.proceed();
        }

        // 如果有MapM2M注解，则这里对结果进行拦截并转换
        MapM2M MapM2MAnnotation = currentMethod.getAnnotation(MapM2M.class);
        Statement statement = (Statement) invocation.getArgs()[0];

        Pair kvTypePair = getGenericOfMap(currentMethod);

        TypeHandlerRegistry typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();// 获取各种TypeHander的注册器
        return result2Map(statement, typeHandlerRegistry, kvTypePair, MapM2MAnnotation);

    }

    @Override
    public Object plugin(Object obj) {
        return Plugin.wrap(obj, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }

    /**
     * 获取指定方法中,返回值Map泛型的个数
     * @param method
     * @return
     */
    private Pair getGenericOfMap(Method method) {
        Type returnType = method.getGenericReturnType();
        if(returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType)returnType;
            if(!Map.class.equals(type.getRawType())) {
                throw new HapException("方法"+method.getName() + "--不合法的返回值类型,该注解的指定放回值类型为Map" );
            }
            Pair pair = recursivePair(new Pair(type.getActualTypeArguments()[0],type.getActualTypeArguments()[1]),type.getActualTypeArguments()[1]);
            return pair;
        }
        return new Pair<>(null, null);
    }
    /**
     * 递归实现,获取泛型Class个数(如果递归层数太深，会造成栈溢出，因此，通常不要超过30层的调用)
     * Pair<Pair<class,Pair<class,...>>,class>
     * @param pair
     * @param type
     * @return
     */
    private  Pair recursivePair(Pair pair,Type type) {
        if(type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType)type;
            if(!Map.class.equals(p.getRawType())) {
                return pair;
            }
            Type first = p.getActualTypeArguments()[0];
            Type second = p.getActualTypeArguments()[1];
            return recursivePair(new Pair(new Pair(pair.getKey(),first),second),second);
        }
        return pair;
    }


    /**
     * 初始化递归调用，只有当层级超过3时,实现递归
     * @param pair
     * @param deep
     * @return 数据库中一条几记录对应的一个返回值Map
     */
    private Map initRecurisiveMap(Pair pair, VALUE deep, ResultSet resultSet, TypeHandlerRegistry typeHandlerRegistry, int column) throws Throwable {
        Map map = new HashMap();
        if(deep == VALUE.FIRST) {
            Object key = this.getObject(resultSet, 1, typeHandlerRegistry,(Class)pair.getKey());
            Object value = this.getObject(resultSet, 2, typeHandlerRegistry,(Class)pair.getValue());
            map.put(pair.getKey(),pair.getValue());
            return map;
        }else if(deep == VALUE.SECOND) {
            Map oneMap = new HashMap();
            Pair lastPair = (Pair)pair.getKey();
            Object lastKey = this.getObject(resultSet, 1, typeHandlerRegistry,(Class)lastPair.getKey());
            Object lastValue = this.getObject(resultSet, 2, typeHandlerRegistry,(Class)lastPair.getValue());
            Object value = this.getObject(resultSet, 3, typeHandlerRegistry,(Class)pair.getValue());
            oneMap.put(lastValue,value);
            map.put(lastKey,oneMap);
            return map;
        } else {
            Pair tempPair = (Pair)pair.getKey();
            Object lastValue = this.getObject(resultSet, column-1, typeHandlerRegistry,(Class)tempPair.getValue());
            Object value = this.getObject(resultSet, 1, typeHandlerRegistry,(Class)pair.getValue());
            map.put(lastValue,value);
            return recursiveMap(map,(Pair)tempPair.getKey(),null,null,null,1);
        }
    }

    /**
     * 递归实现Map数据的构造
     * Map<Integer,Map<String,Long>>
     * Map<1,"2",3L>
     * @param pair
     * @return
     */
    private Map recursiveMap(Map map, Pair pair, Statement statement, TypeHandlerRegistry typeHandlerRegistry, ResultSet resultSet, int i) throws Throwable {
        //至少两层Map嵌套
        if(pair.getKey() instanceof Pair) {
            Map lastMap = new HashMap();
            if(!(((Pair) pair.getKey()).getKey() instanceof Pair)) {
                Map tempMap = new HashMap();
                Map returnMap = new HashMap();
                Pair lastPair = (Pair)pair.getKey();
                Object lastKey = this.getObject(resultSet, i, typeHandlerRegistry,(Class)lastPair.getKey());
                Object lastValue = getObject(resultSet,i+1,typeHandlerRegistry,(Class)lastPair.getValue());
                Object value = getObject(resultSet,i+2,typeHandlerRegistry,(Class)pair.getValue());
                tempMap.put(value,map);
                lastMap.put(lastValue,tempMap);
                returnMap.put(lastKey,lastMap);
                return returnMap;
            }
            Pair keyPair = (Pair)pair.getKey();
            Object value = getObject(resultSet,i,typeHandlerRegistry,(Class)pair.getValue());
            lastMap.put(value,map);
            return recursiveMap(lastMap,keyPair,statement,typeHandlerRegistry,resultSet,++i);
        }
        Object key = this.getObject(resultSet, 1, typeHandlerRegistry,(Class)pair.getKey());
        Object value = getObject(resultSet,2,typeHandlerRegistry,(Class)pair.getValue());
        Map oneMap = new HashMap();
        Map twoMap = new HashMap();
        oneMap.put(value,map);
        twoMap.put(key,oneMap);
        return map;


    }

    /**
     * 找到与指定函数名匹配的Method。
     *
     * @param className
     * @param targetMethodName
     * @return
     * @throws Throwable
     */
    private Method findMethod(String className, String targetMethodName) throws Throwable {
        Method[] methods = Class.forName(className).getDeclaredMethods();// 该类所有声明的方法
        if (methods == null) {
            return null;
        }

        for (Method method : methods) {
            if (StringUtils.equals(method.getName(), targetMethodName)) {
                return method;
            }
        }

        return null;
    }

    /**
     * 将查询结果映射成Map，其中第一个字段作为key，第二个字段作为value.
     *
     * @param statement
     * @param typeHandlerRegistry MyBatis里typeHandler的注册器，方便转换成用户指定的结果类型
     * @param kvTypePair 函数指定返回Map key-value的类型
     * @param MapM2MAnnotation
     * @return
     * @throws Throwable
     */
    private Object result2Map(Statement statement, TypeHandlerRegistry typeHandlerRegistry,
                              Pair kvTypePair, MapM2M MapM2MAnnotation) throws Throwable {
        ResultSet resultSet = statement.getResultSet();
        List<Object> res = new ArrayList();
        Map returnMap = new HashMap();

        // returnMap = recursiveMap(map,kvTypePair);
        //一层MAP
        if(MapM2MAnnotation.deep() == VALUE.FIRST) {
            while (resultSet.next()) {
                Object key = this.getObject(resultSet, 1, typeHandlerRegistry, (Class)kvTypePair.getKey());
                Object value = this.getObject(resultSet, 2, typeHandlerRegistry,(Class)kvTypePair.getValue());
                returnMap.put(key,value);
            }
            //二层
        } else if(MapM2MAnnotation.deep() == VALUE.SECOND) {
            while (resultSet.next()) {
                Map innerMap = new HashMap();
                //returnMap = recursiveMap(innerMap,kvTypePair,statement,typeHandlerRegistry,resultSet,1);
                Object key = this.getObject(resultSet, 1, typeHandlerRegistry, (Class)(((Pair)kvTypePair.getKey()).getKey()));
                Object secondKey = this.getObject(resultSet, 2, typeHandlerRegistry,(Class)(((Pair)kvTypePair.getKey()).getValue()));
                Object secondValue = this.getObject(resultSet, 3, typeHandlerRegistry,(Class) kvTypePair.getValue());
                innerMap.put(secondKey,secondValue);
                returnMap.put(key,innerMap);
            }
        }
        res.add(returnMap);
        return res;
    }

    /**
     * 结果类型转换。
     * <p>
     * 这里借用注册在MyBatis的typeHander（包括自定义的），方便进行类型转换。
     *
     * @param resultSet
     * @param columnIndex 字段下标，从1开始
     * @param typeHandlerRegistry MyBatis里typeHandler的注册器，方便转换成用户指定的结果类型
     * @param javaType 要转换的Java类型
     * @return
     * @throws SQLException
     */
    private Object getObject(ResultSet resultSet, int columnIndex, TypeHandlerRegistry typeHandlerRegistry,
                             Class<?> javaType) throws SQLException {
        final TypeHandler<?> typeHandler = typeHandlerRegistry.hasTypeHandler(javaType)
                ? typeHandlerRegistry.getTypeHandler(javaType) : typeHandlerRegistry.getUnknownTypeHandler();

        return typeHandler.getResult(resultSet, columnIndex);

    }

}
