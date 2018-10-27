package MapM2M.annotation;

import MapM2M.constant.VALUE;

import java.lang.annotation.*;

/**
 * @author jianyuan.wei@hand-china.com
 * @date 2018/10/13 23:30
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface MapM2M {
    /**
     * 嵌套Map的深度是几层,最多不要超过三层
     * Map<Integer,<Integer,String>> -- 2层嵌套
     * @return
     */
    VALUE deep() default VALUE.FIRST;
    /**
     * 是否允许key重复。如果不允许,而实际结果出现重复，会抛出
     * org.springframework.dao.duplicateKeyException
     * @return
     */
    boolean isAllowKeyRepeat() default true;

    /**
     * 对于相同的key,是否允许value不同(在允许key重复的前提下)，如果允许，则按
     * 查询结果，后面覆盖前面；如果不允许，则会抛出异常
     * @return
     */
    boolean isAllowValueDifferentWithSameKey() default false;

}
