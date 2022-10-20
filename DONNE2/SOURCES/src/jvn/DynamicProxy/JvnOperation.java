package jvn.DynamicProxy;
import java.lang.annotation.*; 

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JvnOperation {
    String type();
}
