package org.noear.fairy;

import org.noear.fairy.annotation.Alias;
import org.noear.fairy.annotation.FairyClient;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fairy - 调用处理程序
 *
 * @author noear
 * @since 1.0
 * */
public class FairyHandler implements InvocationHandler {
    private final FairyConfig config;

    private final Map<String,String> headers0 = new LinkedHashMap<>();
    private final String name0;
    private final String path0;
    private final String url0;

    /**
     * @param config 配置
     * @param client 客户端注解
     * */
    public FairyHandler(Class<?> clz, FairyConfig config, FairyClient client) {
        this.config = config;

        //1.运行配置器
        if (client != null) {
            try {
                FairyConfiguration tmp = client.configuration().newInstance();

                if (tmp != null) {
                    tmp.config(client, new Fairy.Builder(config));
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

            //>>添加接口header
            if (client.headers() != null) {
                for (String h : client.headers()) {
                    String[] ss = h.split("=");
                    headers0.put(ss[0], ss[1]);
                }
            }
        }

        //2.配置初始化
        config.tryInit();

        //3.获取 or url
        String uri = config.getUri();

        if (uri == null && client != null) {
            //1.优先从 XClient 获取服务地址或名称
            if (isEmpty(client.value()) == false) {
                uri = client.value();
            }
        }

        //2.如果没有，就报错
        if (uri == null) {
            throw new FairyException("FairyClient config is wrong: " +clz.getName());
        }

        if (uri.contains("://")) {
            url0 = uri;
            name0 = null;
            path0 = null;
        } else {
            if (uri.contains(":")) {
                url0 = null;
                name0 = uri.split(":")[0];
                path0 = uri.split(":")[1];
            } else {
                url0 = null;
                name0 = null;
                path0 = uri;
            }
        }

        if( url0 == null && config.getUpstream() == null){
            throw new FairyException("FairyClient: Not found upstream: " +clz.getName());
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] vals) throws Throwable {
        //构建 fun
        String fun = method.getName();
        Alias alias = method.getAnnotation(Alias.class);
        if (alias != null && isEmpty(alias.value()) == false) {
            fun = alias.value();
        }

        //构建 args
        Map<String, Object> args = new LinkedHashMap<>();
        Parameter[] names = method.getParameters();
        for (int i = 0, len = names.length; i < len; i++) {
            if (vals[i] != null) {
                args.put(names[i].getName(), vals[i]);
            }
        }

        //构建 headers
        Map<String,String> headers = new HashMap<>(headers0);

        //构建 url
        String url = null;
        if (url0 == null) {
            url = config.getUpstream().getServer();

            if (url == null) {
                throw new FairyException("FairyClient: Not found upstream!");
            }

            if (path0 != null) {
                int idx = url.indexOf("/", 9);//https://a
                if (idx > 0) {
                    url = url.substring(0, idx);
                }

                if (path0.endsWith("/")) {
                    fun = path0 + fun;
                } else {
                    fun = path0 + "/" + fun;
                }
            }

        } else {
            url = url0;
        }


        //执行调用
        return new Fairy(config)
                .url(url, fun)
                .call(headers, args)
                .getObject(method.getReturnType());
    }


    private static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }
}
