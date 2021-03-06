package org.noear.solon.core;

import org.noear.solon.XApp;
import org.noear.solon.XUtil;
import org.noear.solon.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Aop 处理工厂（可以被继承重写）
 *
 * 主要实现三个动作：
 * 1.生成 bean
 * 2.构建 bean
 * 3.注入 字段或参数
 *
 * @author noear
 * @since 1.0
 * */
public class AopFactory extends AopFactoryBase {


    public AopFactory() {
        initialize();
    }

    /**
     * ::初始化（独立出 initialize，方便重写）
     */
    protected void initialize() {

        beanCreatorAdd(XConfiguration.class, (clz, bw, anno) -> {
            XInject typeInj = clz.getAnnotation(XInject.class);
            if (typeInj != null && XUtil.isNotEmpty(typeInj.value())) {
                if (typeInj.value().startsWith("${")) {
                    Aop.inject(bw.raw(), XApp.cfg().getPropByExpr(typeInj.value()));
                }
            }

            for (MethodWrap mWrap : ClassWrap.get(bw.clz()).methodWraps) {
                XBean m_an = mWrap.getMethod().getAnnotation(XBean.class);

                if (m_an != null) {
                    XInject beanInj = mWrap.getMethod().getAnnotation(XInject.class);

                    tryBuildBean(m_an, mWrap, bw, beanInj, (p1) -> {
                        XInject tmp = p1.getAnnotation(XInject.class);
                        if (tmp == null) {
                            return null;
                        } else {
                            return tmp.value();
                        }
                    });
                }
            }

            //添加bean形态处理
            addBeanShape(clz, bw);
        });

        beanCreatorAdd(XBean.class, (clz, bw, anno) -> {
            bw.nameSet(anno.value());
            bw.tagSet(anno.tag());
            bw.attrsSet(anno.attrs());
            bw.typedSet(anno.typed());

            //添加bean形态处理
            addBeanShape(clz, bw);

            //设置remoting状态
            bw.remotingSet(anno.remoting());

            //注册到管理中心
            beanRegister(bw, anno.value(), anno.typed());

            //如果是remoting状态，转到XApp路由器
            if (bw.remoting()) {
                BeanWebWrap bww = new BeanWebWrap(bw);
                if (bww.mapping() != null) {
                    //
                    //如果没有xmapping，则不进行web注册
                    //
                    bww.load(XApp.global());
                }
            }
        });

        beanCreatorAdd(XController.class, (clz, bw, anno) -> {
            new BeanWebWrap(bw).load(XApp.global());
        });

        beanCreatorAdd(XInterceptor.class, (clz, bw, anno) -> {
            new BeanWebWrap(bw).main(false).load(XApp.global());
        });

        beanInjectorAdd(XInject.class, ((fwT, anno) -> {
            tryInjectByName(fwT, anno.value());
        }));

    }

    private void addBeanShape(Class<?> clz, BeanWrap bw){
        //XPlugin
        if (XPlugin.class.isAssignableFrom(bw.clz())) {
            //如果是插件，则插入
            XApp.global().plug(bw.raw());
            return;
        }

        //XEventListener
        if (XEventListener.class.isAssignableFrom(clz)) {
            addEventListener(clz, bw);
            return;
        }

        //XUpstreamFactory
        if(XUpstreamFactory.class.isAssignableFrom(clz)){
            XBridge.upstreamFactorySet(bw.raw());
        }
    }

    private void addEventListener(Class<?> clz, BeanWrap bw) {
        for (Type t1 : clz.getGenericInterfaces()) {
            if (t1 instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t1;
                if (pt.getRawType() == XEventListener.class) {
                    Class<?> et = (Class<?>) pt.getActualTypeArguments()[0];
                    XEventBus.subscribe(et, bw.raw());
                    return;
                }
            }
        }
    }

    /**
     * ::扫描源下的所有 bean 及对应处理
     */
    public void beanScan(Class<?> source) {
        //确定文件夹名
        String dir = "";
        if (source.getPackage() != null) {
            dir = source.getPackage().getName().replace('.', '/');
        }

        //扫描类文件并处理（采用两段式加载，可以部分bean先处理；剩下的为第二段处理）
        XScaner.scan(dir, n -> n.endsWith(".class"))
                .stream().sorted(Comparator.comparing(s -> s.length())).forEach(name -> {
            String className = name.substring(0, name.length() - 6);

            Class<?> clz = XUtil.loadClass(className.replace("/", "."));
            if (clz != null) {
                tryCreateBean(clz);
            }
        });
    }

    /**
     * ::制造当前 bean 及对应处理
     * */
    public BeanWrap beanMake(Class<?> clz) {
        //包装
        BeanWrap bw = wrap(clz, null);

        tryCreateBean(bw);

        //尝试入库
        putWrap(clz, bw);

        return bw;
    }

    /**
     * 完成加载时调用，会进行事件通知
     * */
    public void beanLoaded(){
        //尝试加载事件（不用函数包装，是为了减少代码）
        loadedEvent.forEach(f -> f.run());
    }


    /**
     * 注册到管理中心
     */
    public void beanRegister(BeanWrap bw, String name, boolean typed) {
        if (XUtil.isNotEmpty(name)) {
            //有name的，只用name注入
            //
            Aop.factory().putWrap(name, bw);
            if (typed == false) {
                //如果非typed，则直接返回
                return;
            }
        }

        Aop.factory().putWrap(bw.clz(), bw);
        Aop.factory().putWrap(bw.clz().getName(), bw);

        //如果有父级接口，则建立关系映射
        Class<?>[] list = bw.clz().getInterfaces();
        for (Class<?> c : list) {
            if (c.getName().contains("java.") == false) {
                //建立关系映射
                clzMapping.putIfAbsent(c, bw.clz());
                Aop.factory().putWrap(c, bw);
            }
        }
    }

    //::注入

    /**
     * 为一个对象注入（可以重写）
     */
    public void beanInject(Object obj) {
        if (obj == null) {
            return;
        }

        ClassWrap clzWrap = ClassWrap.get(obj.getClass());

        //支持父类注入
        for (Map.Entry<String, FieldWrap> kv : clzWrap.fieldAll().entrySet()) {
            Annotation[] annS = kv.getValue().field.getDeclaredAnnotations();
            if (annS.length > 0) {
                VarHolder varH = kv.getValue().holder(obj);
                tryInject(varH, annS);
            }
        }
    }
}
