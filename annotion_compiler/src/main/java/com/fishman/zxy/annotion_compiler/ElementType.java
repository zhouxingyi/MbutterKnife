package com.fishman.zxy.annotion_compiler;

import java.util.List;


import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;


public class ElementType {
    // TypeElement 类节点
    //ExecutableElement 方法节点
    //VariableElement  成员变量节点

    List<VariableElement> viewElements;
    List<VariableElement> StringElements;
    List<ExecutableElement> methodElements;

    public List<VariableElement> getViewElements() {
        return viewElements;
    }

    public void setViewElements(List<VariableElement> viewElements) {
        this.viewElements = viewElements;
    }

    public List<VariableElement> getStringElements() {
        return StringElements;
    }

    public void setStringElements(List<VariableElement> stringElements) {
        StringElements = stringElements;
    }

    public List<ExecutableElement> getMethodElements() {
        return methodElements;
    }

    public void setMethodElements(List<ExecutableElement> methodElements) {
        this.methodElements = methodElements;
    }
}
