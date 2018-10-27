# MapM2M

该工程主要是对mybatis的结果集进行拦截,并实现将查询结果和Map的key,value进行映射，
共有两种返回方式：
  1.Map<xx,xxx>
  2.Map<xx,Map<xxx,xxx>>  
 用法：
  @MapM2M(deep=VALUE.SECOND)
  Map<String,Map<String,String>> queryMapTest();
