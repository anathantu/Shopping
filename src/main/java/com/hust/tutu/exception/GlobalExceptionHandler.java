package com.hust.tutu.exception;


import com.hust.tutu.result.CodeMsg;
import com.hust.tutu.result.Result;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import org.springframework.validation.BindException;
import java.util.List;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public Result<String> exceptionHandler(HttpServletRequest request,Exception e){
        e.printStackTrace();
        if(e instanceof GlobalException){
            GlobalException ex= (GlobalException)e;
            return Result.error(ex.getCm());
        }else if(e instanceof BindException){
            BindException ex=(BindException) e;
            List<ObjectError> errors=ex.getAllErrors();
            ObjectError error=errors.get(0);
            String msg = error.getDefaultMessage();
            System.out.println("GlobalException: "+msg);
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        }else{
            return Result.error(CodeMsg.SERVER_ERROR);
        }

    }
}
