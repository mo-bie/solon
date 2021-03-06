package org.noear.solon.boot.jdksocket;

import org.noear.solon.XApp;
import org.noear.solon.core.SocketMessage;
import org.noear.solon.core.XEventBus;

public class SocketContextHandler {

    public void handle(SocketSession session, SocketMessage message) {
        if (message == null) {
            return;
        }

        try {
            handleDo(session, message);
        } catch (Throwable ex) {
            //context 初始化时，可能会出错
            //
            XEventBus.push(ex);
        }
    }

    private void handleDo(SocketSession session, SocketMessage message) {
        SocketContext context = new SocketContext(session, message);

        try {
            XApp.global().tryHandle(context);

            context.commit();
        } catch (Throwable ex) {
            XEventBus.push(ex);
        }
    }
}
