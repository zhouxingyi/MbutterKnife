package com.fishman.zxy.annotion_compiler;

import com.fishman.zxy.annotion_lib.BindString;
import com.fishman.zxy.annotion_lib.BindView;
import com.fishman.zxy.annotion_lib.OnClick;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;


@AutoService(Processor.class)
public class AnnotionCopiler extends AbstractProcessor {
    private Filer filer;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        filer=processingEnvironment.getFiler();
    }

    /**
     * 注解处理器支持的java原版本
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return processingEnv.getSourceVersion();
    }

    /**
     * 声明注解处理器要处理的注解
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set=new HashSet<>();
        set.add(BindView.class.getCanonicalName());
        set.add(BindString.class.getCanonicalName());
        set.add(OnClick.class.getCanonicalName());
        return set;

    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //获取所有的注解，并且用注解的类对象作为key，把类中的注解和类一一对应起来并添加到集合
        Map<TypeElement,ElementType> map=findAndAnalyze(roundEnvironment);
        if(map.size()>0){
            Iterator<TypeElement> iterator = map.keySet().iterator();
            Writer writer = null;

            //遍历所有的key,并自动生成一个个工具类
            while (iterator.hasNext()){
               TypeElement typeElement=iterator.next();
                ElementType elementType=map.get(typeElement);
                String activityName=typeElement.getSimpleName().toString();
                String newClassName=activityName+"$$ViewBinder";
                String packageName=getPackageName(typeElement);
                try {
                    //更具包名 和工具类名 创建工具类文件
                    JavaFileObject source=filer.createSourceFile(packageName+"."+newClassName);
                    writer=source.openWriter();
                    StringBuffer stringBuffer= getCodeStringBuff(packageName,newClassName,typeElement,elementType);
                    logUtil(stringBuffer.toString());
                    writer.write(stringBuffer.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    if(writer != null){
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return false;
    }

    private StringBuffer getCodeStringBuff(String packageName, String newClassName, TypeElement typeElement, ElementType elementType) {
       StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("package "+packageName+";\n");
        stringBuffer.append("import android.view.View;\n");
        stringBuffer.append("public class "+newClassName+"{\n");
        stringBuffer.append("public "+newClassName+"(final "+typeElement.getQualifiedName()+" target){\n");

        if(elementType!=null && elementType.getViewElements()!=null && elementType.getViewElements().size()>0){
            List<VariableElement> viewElements = elementType.getViewElements();
            for (VariableElement viewElement : viewElements) {
                TypeMirror typeMirror = viewElement.asType();
                Name simpleName = viewElement.getSimpleName();
                int resId = viewElement.getAnnotation(BindView.class).value();
                stringBuffer.append("target."+simpleName +" =(" +typeMirror+")target.findViewById("+resId+");\n");
            }
        }
        if(elementType!=null && elementType.getStringElements()!=null && elementType.getStringElements().size()>0){
            List<VariableElement> viewElements = elementType.getStringElements();
            for (VariableElement viewElement : viewElements) {
                TypeMirror typeMirror = viewElement.asType();
                Name simpleName = viewElement.getSimpleName();
                int resId = viewElement.getAnnotation(BindString.class).value();
                stringBuffer.append("target."+simpleName +"=target.getResources().getString("+resId+");\n");
            }
        }
        if(elementType!=null && elementType.getMethodElements()!=null && elementType.getMethodElements().size()>0){
            List<ExecutableElement> methodElements = elementType.getMethodElements();
            for (ExecutableElement methodElement : methodElements) {
                int[] resIds = methodElement.getAnnotation(OnClick.class).value();
                String methodName = methodElement.getSimpleName().toString();
                for (int resId : resIds) {
                    stringBuffer.append("(target.findViewById("+resId+")).setOnClickListener(new View.OnClickListener() {\n");
                    stringBuffer.append("public void onClick(View p0) {\n");
                    stringBuffer.append("target."+methodName+"(p0);\n");
                    stringBuffer.append("}\n});\n");
                }
            }
        }
        stringBuffer.append("}\n}\n");

       return stringBuffer;
    }

    /**
     * 获取包名
     * @param typeElement
     * @return
     */
    private String getPackageName(TypeElement typeElement) {
        String packagename;
        PackageElement packageOf = processingEnv.getElementUtils().getPackageOf(typeElement);
        packagename = packageOf.getQualifiedName().toString();
        return packagename;
    }

    /**
     * 获取注解 和 注解所在的类 添加到集合
     * @param roundEnvironment
     * @return
     */
    private Map<TypeElement, ElementType> findAndAnalyze(RoundEnvironment roundEnvironment) {
        // TypeElement 类节点
        //ExecutableElement 方法节点
        //VariableElement  成员变量节点

        Map<TypeElement, ElementType> maps =new HashMap<>();
        //获取模块用到的 BindView  BindString  OnClick 的节点
        Set<? extends Element> viewElements=roundEnvironment.getElementsAnnotatedWith(BindView.class);
        Set<? extends Element> StringElements=roundEnvironment.getElementsAnnotatedWith(BindString.class);
        Set<? extends Element> methodElements=roundEnvironment.getElementsAnnotatedWith(OnClick.class);
        for (Element element: viewElements) {
            //获取注解的成员变量
            VariableElement variableElement=(VariableElement)element;
            //获取注解的所在类
            TypeElement typeElement= (TypeElement) variableElement.getEnclosingElement();
            //保存各种注解的集合
            ElementType elementType=maps.get(typeElement);
            List<VariableElement> viewEvents;
            if(elementType!=null){
                //获取BindView 类型的注解集合
                viewEvents=elementType.getViewElements();
                if(viewEvents==null){
                    viewEvents=new ArrayList<>();
                    elementType.setViewElements(viewEvents);
                }
            }else{
                elementType=new ElementType();
                viewEvents=new ArrayList<>();
                elementType.setViewElements(viewEvents);
                if(!maps.containsKey(typeElement)){
                    maps.put(typeElement,elementType);
                }
            }
            viewEvents.add(variableElement);
        }
        for (Element element: StringElements) {
            VariableElement variableElement=(VariableElement)element;
            TypeElement typeElement= (TypeElement) variableElement.getEnclosingElement();
            ElementType elementType=maps.get(typeElement);
            List<VariableElement> StringEvents;
            if(elementType!=null){
                //获取BindString 类型的注解集合
                StringEvents=elementType.getStringElements();
                if(StringEvents==null){
                    StringEvents=new ArrayList<>();
                    elementType.setStringElements(StringEvents);
                }
            }else{
                elementType=new ElementType();
                StringEvents=new ArrayList<>();
                elementType.setStringElements(StringEvents);
                if(!maps.containsKey(typeElement)){
                    maps.put(typeElement,elementType);
                }
            }
            StringEvents.add(variableElement);
        }
        for (Element element: methodElements) {
            ExecutableElement executableElement=(ExecutableElement)element;
            TypeElement typeElement= (TypeElement) executableElement.getEnclosingElement();
            ElementType elementType=maps.get(typeElement);
            List<ExecutableElement> methodEvents;
            if(elementType!=null){
                //获取OnClick 类型的注解集合
                methodEvents=elementType.getMethodElements();
                if(methodEvents==null){
                    methodEvents=new ArrayList<>();
                    elementType.setMethodElements(methodEvents);
                }
            }else{
                elementType=new ElementType();
                methodEvents=new ArrayList<>();
                elementType.setMethodElements(methodEvents);
                if(!maps.containsKey(typeElement)){
                    maps.put(typeElement,elementType);
                }
            }
            methodEvents.add(executableElement);
        }
        return  maps;
    }
    public void logUtil(String message){
        Messager messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE,message);
    }
}
