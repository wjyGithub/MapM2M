# MapM2M

注意：gitHub在编辑文件是，如果想要正确换行，需要在该行尾添加两个空格

该工程主要是对mybatis的结果集进行拦截,并实现将查询结果和Map的key,value进行映射，
共有两种返回方式：
  1.Map<xx,xxx>  
  2.Map<xx,Map<xxx,xxx>>  
 用法:  
  @MapM2M(deep=VALUE.SECOND)  
  Map<String,Map<String,String>> queryMapTest();
  
  在springboot里面进行使用时,需要进行配置  
  
  @Configuration
  public class config {  
      @Bean  
      public MapF2FInterceptor getMapF2FInterceptor(){  
          return new MapF2FInterceptor();  
      }  
  }  
