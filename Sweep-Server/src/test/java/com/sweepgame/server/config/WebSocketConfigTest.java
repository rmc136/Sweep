package com.sweepgame.server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    // -- configureMessageBroker -----------------------------------------------

    @Test
    void configureMessageBroker_enablesSimpleBrokerOnTopicAndQueue() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        webSocketConfig.configureMessageBroker(registry);

        verify(registry).enableSimpleBroker("/topic", "/queue");
    }

    @Test
    void configureMessageBroker_setsApplicationDestinationPrefixToApp() {
        MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

        webSocketConfig.configureMessageBroker(registry);

        verify(registry).setApplicationDestinationPrefixes("/app");
    }

    // -- registerStompEndpoints -----------------------------------------------

    @Test
    void registerStompEndpoints_registersWsEndpoint() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(registry);

        verify(registry).addEndpoint("/ws");
    }

    @Test
    void registerStompEndpoints_allowsAllOriginPatterns() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(registry);

        verify(registration).setAllowedOriginPatterns("*");
    }

    @Test
    void registerStompEndpoints_enablesSockJS() {
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(registration);
        when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

        webSocketConfig.registerStompEndpoints(registry);

        verify(registration).withSockJS();
    }

    // -- channel interceptor helpers ------------------------------------------

    /**
     * Registers the inbound-channel interceptor and returns the captured instance
     * so individual tests can call preSend() directly.
     */
    private ChannelInterceptor captureInterceptor() {
        AtomicReference<ChannelInterceptor> captured = new AtomicReference<>();
        ChannelRegistration registration = mock(ChannelRegistration.class);
        doAnswer(invocation -> {
            captured.set(invocation.getArgument(0));
            return registration;
        }).when(registration).interceptors(any(ChannelInterceptor.class));

        webSocketConfig.configureClientInboundChannel(registration);
        assertNotNull(captured.get(), "No interceptor was registered");
        return captured.get();
    }

    private Message<byte[]> buildConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> buildSendMessage() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/game");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // -- interceptor: CONNECT with valid token --------------------------------

    @Test
    void interceptor_connect_validBearerToken_setsUserOnAccessor() {
        when(jwtConfig.extractUsername("tok123")).thenReturn("alice");
        when(jwtConfig.isTokenExpired("tok123")).thenReturn(false);
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(
                buildConnectMessage("Bearer tok123"), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(resultAccessor.getUser(), "User should be set for a valid token");
        assertEquals("alice", resultAccessor.getUser().getName());
    }

    // -- interceptor: CONNECT with expired token ------------------------------

    @Test
    void interceptor_connect_expiredToken_doesNotSetUser() {
        when(jwtConfig.extractUsername("expiredTok")).thenReturn("bob");
        when(jwtConfig.isTokenExpired("expiredTok")).thenReturn(true);
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(
                buildConnectMessage("Bearer expiredTok"), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(resultAccessor.getUser(), "User must not be set for an expired token");
    }

    // -- interceptor: CONNECT with null username from jwtConfig ---------------

    @Test
    void interceptor_connect_nullUsername_doesNotSetUser() {
        when(jwtConfig.extractUsername("nullUserTok")).thenReturn(null);
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(
                buildConnectMessage("Bearer nullUserTok"), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(resultAccessor.getUser(), "User must not be set when username is null");
    }

    // -- interceptor: CONNECT with no Authorization header --------------------

    @Test
    void interceptor_connect_noAuthHeader_doesNotSetUser() {
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(
                buildConnectMessage(null), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(resultAccessor.getUser());
    }

    // -- interceptor: CONNECT with non-Bearer scheme --------------------------

    @Test
    void interceptor_connect_basicAuthHeader_doesNotSetUser() {
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(
                buildConnectMessage("Basic dXNlcjpwYXNz"), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(resultAccessor.getUser());
    }

    // -- interceptor: CONNECT with invalid/tampered token ---------------------

    @Test
    void interceptor_connect_invalidToken_swallowsExceptionAndDoesNotSetUser() {
        when(jwtConfig.extractUsername("bad.token.here"))
                .thenThrow(new RuntimeException("JWT parse failure"));
        ChannelInterceptor interceptor = captureInterceptor();

        // Must not propagate the exception
        Message<?> result = assertDoesNotThrow(() ->
                interceptor.preSend(buildConnectMessage("Bearer bad.token.here"),
                        mock(MessageChannel.class)));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(resultAccessor.getUser());
    }

    // -- interceptor: non-CONNECT frames pass through unchanged ---------------

    @Test
    void interceptor_nonConnectFrame_passesThroughWithoutSettingUser() {
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(buildSendMessage(), mock(MessageChannel.class));

        assertNotNull(result);
        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNull(resultAccessor.getUser());
        // jwtConfig must never be consulted for non-CONNECT frames
        verifyNoInteractions(jwtConfig);
    }

    // -- interceptor: always returns the message (never null) -----------------

    @Test
    void interceptor_connect_validToken_returnsMessageNotNull() {
        when(jwtConfig.extractUsername("tok456")).thenReturn("carol");
        when(jwtConfig.isTokenExpired("tok456")).thenReturn(false);
        ChannelInterceptor interceptor = captureInterceptor();

        Message<?> result = interceptor.preSend(
                buildConnectMessage("Bearer tok456"), mock(MessageChannel.class));

        assertNotNull(result, "preSend must never return null â€” a null drops the message");
    }

    // BUG FOUND: if MessageHeaderAccessor.getAccessor() returns null for a
    // non-STOMP message (e.g. a plain SimpMessageType.HEARTBEAT), then
    // accessor.getCommand() on line ~44 of WebSocketConfig will throw a
    // NullPointerException because there is no null-guard on `accessor` before
    // dereferencing it.
}
