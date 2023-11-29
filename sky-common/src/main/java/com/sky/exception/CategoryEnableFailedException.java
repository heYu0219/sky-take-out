package com.sky.exception;

/**
 * 分类启用失败异常
 */
public class CategoryEnableFailedException extends BaseException {

    public CategoryEnableFailedException(){}

    public CategoryEnableFailedException(String msg){
        super(msg);
    }
}
