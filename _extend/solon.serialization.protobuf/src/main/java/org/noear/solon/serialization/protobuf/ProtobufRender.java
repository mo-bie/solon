package org.noear.solon.serialization.protobuf;

import io.edap.x.protobuf.ProtoBuf;
import org.noear.solon.core.ModelAndView;
import org.noear.solon.core.XContext;
import org.noear.solon.core.XRender;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

//不要要入参，方便后面多视图混用
//
public class ProtobufRender implements XRender {

    @Override
    public void render(Object obj, XContext ctx) throws Throwable {
        if(XPluginImp.output_meta) {
            ctx.headerSet("solon.serialization", "ProtobufRender");
        }

        ctx.contentType("application/protobuf");

        byte[] bytes = null;
        if(obj instanceof ModelAndView){
            bytes = ProtoBuf.ser(new HashMap<>(((Map)obj)));
        }else{
            bytes = ProtoBuf.ser(obj);
        }

        ctx.output(new ByteArrayInputStream(bytes));
    }
}
