package com.amnedev.p3rmenu.util;

/** Read-only bridge used to choose the chat entrance without exposing Minecraft internals. */
public interface P3RChatHistoryState {
    boolean p3r_hasChatMessages();
}
