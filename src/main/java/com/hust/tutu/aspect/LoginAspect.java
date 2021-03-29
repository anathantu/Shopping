package com.hust.tutu.aspect;

import com.hust.tutu.domain.MiaoshaUser;
import com.hust.tutu.exception.GlobalException;
import com.hust.tutu.result.CodeMsg;
import com.hust.tutu.validator.NeedLogin;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoginAspect {

    @Pointcut("@annotation(com.hust.tutu.validator.NeedLogin)")
    public void loginAspect(){}

    @Before("loginAspect()&&@annotation(needLogin)")
    public void doBefore(JoinPoint joinPoint, NeedLogin needLogin) throws ClassNotFoundException {
//        String targetName = joinPoint.getTarget().getClass().getName();
//        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        int pos = needLogin.paramPos();
        if(args[pos-1]==null || args[pos-1].getClass() != MiaoshaUser.class)
            throw new GlobalException(CodeMsg.USER_NOT_LOGIN);
//        Class targetClass = Class.forName(targetName);
//        Method[] methods = targetClass.getMethods();
//        for (Method method : methods) {
//            if (method.getName().equals(methodName)) {
//                Class[] clazzs = method.getParameterTypes();
//                if (clazzs.length == arguments.length) {
//                }
//            }
//        }
    }
}
