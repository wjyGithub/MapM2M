# MapM2M

注意：gitHub在编辑文件是，如果想要正确换行，需要在该行尾添加两个空格

该工程主要是对mybatis的结果集进行拦截,并实现将查询结果和Map的key,value进行映射，  
共有两种返回方式：  
  1.Map<xx,xxx>  
  2.Map<xx,Map<xxx,xxx>>  
 用法: 
```
假设queryMapTest查询的结果为:
    ID   name   age 
    1    wjy1   21  
    2    wiy2   22  
    
  @MapM2M(deep=VALUE.SECOND)  
  Map<String,Map<String,String>> queryMapTest();
  返回结果:
  Map:
  {1,{wjy1,21}}
  {2,{wjy2,22}}
  即：
   Map returnMap = new HashMap<>();
   Map firstRecord = new HashMap<>();
   firstRecord.put("wjy1",21);
   returnMap.put(1,firstRecord);
   Map secondRecord = new HashMap<>();
   secondRecord.put("wjy2",22);
   returnMap.put(2,secondRecord);   
```
  在springboot里面进行使用时,需要进行配置  
```  
  @Configuration  
  public class config {  
      @Bean  
      public MapM2MInterceptor getMapM2MInterceptor(){    
                return new MapF2FInterceptor();  
        }  
  } 
```
