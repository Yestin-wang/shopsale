package com.yestin.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@ServerEndpoint("/{token}")
@Slf4j
public class WebsocketServer {
    // 当前在线连接数，使用原子操作保证线程安全
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);
    // 所有客户端的会话映射表
    public static final Map<String, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立时调用
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        // 检查token有效性
        if (token == null || token.isEmpty()) {
            log.warn("[WebSocket] 无效的token连接请求被拒绝");
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "无效的token"));
            } catch (IOException e) {
                log.error("[WebSocket] 关闭无效连接失败", e);
            }
            return;
        }

        // 更新在线数统计并记录日志
        int onlineCount = ONLINE_COUNT.incrementAndGet();
        log.info("[WebSocket] 新的客户连接: token={}, 当前在线数: {}", token, onlineCount);

        // 如果已存在相同token的连接，先关闭旧连接
        Session oldSession = SESSION_MAP.get(token);
        if (oldSession != null && oldSession.isOpen()) {
            try {
                log.info("[WebSocket] 检测到相同token的旧连接，准备关闭: {}", token);
                oldSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "建立了新连接"));
            } catch (IOException e) {
                log.warn("[WebSocket] 关闭旧连接时出错", e);
            } finally {
                // 确保从映射表中移除旧会话
                SESSION_MAP.remove(token);
            }
        }

        // 配置会话超时 - 10分钟超时
        session.setMaxIdleTimeout(10 * 60 * 1000);

        // 保存连接对象
        SESSION_MAP.put(token, session);

        try {
            // 发送连接成功消息
            session.getBasicRemote().sendText("{\"type\":\"connect\",\"status\":\"success\",\"message\":\"连接已建立\"}");
        } catch (IOException e) {
            log.error("[WebSocket] 发送连接成功消息失败", e);
        }

        // 调试输出当前所有会话
        log.debug("[WebSocket] 当前会话映射: {}", SESSION_MAP.keySet());

        log.info("[WebSocket] 连接建立，token={}", token);
    }

    /**
     * 连接关闭时调用
     */
    @OnClose
    public void onClose(@PathParam("token") String token) {
        // 先判断token是否存在，避免重复移除
        if (SESSION_MAP.containsKey(token)) {
            // 从映射表中移除
            SESSION_MAP.remove(token);
            // 更新在线数并记录日志
            int onlineCount = ONLINE_COUNT.decrementAndGet();
            log.info("[WebSocket] 连接关闭: token={}, 当前在线数: {}", token, onlineCount);
        }
    }

    /**
     * 收到客户端消息时调用
     */
    @OnMessage
    public void onMessage(@PathParam("token") String token, String message) {
        // 处理心跳消息
        if ("ping".equals(message)) {
            try {
                Session session = SESSION_MAP.get(token);
                if (session != null && session.isOpen()) {
                    session.getBasicRemote().sendText("pong");
                    log.debug("[WebSocket] 响应心跳消息: token={}", token);
                }
            } catch (IOException e) {
                log.error("[WebSocket] 发送心跳响应失败: token={}", token, e);
            }
        }
    }

    /**
     * 连接发生错误时调用
     */
    @OnError
    public void onError(Session session, @PathParam("token") String token, Throwable throwable) {
        log.error("[WebSocket] 连接异常: token={}, error={}", token, throwable.getMessage(), throwable);

        if (token != null) {
            // 从会话映射表中移除
            Session mappedSession = SESSION_MAP.remove(token);
            if (mappedSession != null) {
                // 只有当确实移除了会话时才减少计数
                ONLINE_COUNT.decrementAndGet();
            }
        }

        // 关闭会话
        if (session != null && session.isOpen()) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "服务器错误"));
            } catch (IOException e) {
                log.error("[WebSocket] 关闭异常连接失败", e);
            }
        }
    }

    /**
     * 发送消息到指定客户端
     */
    public static boolean sendMessage(String token, String message) {
        if (token == null || token.isEmpty() || message == null) {
            log.warn("[WebSocket] 发送消息失败: 无效的token或消息内容");
            return false;
        }

        Session session = SESSION_MAP.get(token);
        if (session == null) {
            log.warn("[WebSocket] 发送消息失败: 未找到token对应的会话, token={}", token);
            return false;
        }

        if (!session.isOpen()) {
            log.warn("[WebSocket] 发送消息失败: 会话已关闭, token={}", token);
            // 确保将关闭的会话从映射表中移除
            SESSION_MAP.remove(token);
            return false;
        }

        try {
            synchronized (session) {
                // 确保消息发送是线程安全的
                session.getBasicRemote().sendText(message);
            }
            log.debug("[WebSocket] 消息发送成功: token={}", token);
            return true;
        } catch (IOException e) {
            log.error("[WebSocket] 发送消息异常: token={}, error={}", token, e.getMessage(), e);
            // 出现异常时移除会话
            SESSION_MAP.remove(token);
            try {
                // 尝试关闭有问题的会话
                if (session.isOpen()) {
                    session.close(new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR, "消息发送失败"));
                }
            } catch (IOException ex) {
                log.error("[WebSocket] 关闭异常会话失败", ex);
            }
            return false;
        }
    }

    /**
     * 获取当前在线连接数
     */
    public static int getOnlineCount() {
        return ONLINE_COUNT.get();
    }
}
