package com.softartdev.ktlan.data.webrtc

enum class P2pState {
    /**
     * Initialization in progress.
     */
    INITIALIZING,
    /**
     * App is waiting for offer, fill in the offer into the edit text.
     */
    WAITING_FOR_OFFER,
    /**
     * App is creating the offer.
     */
    CREATING_OFFER,
    /**
     * App is creating answer to offer.
     */
    CREATING_ANSWER,
    /**
     * App created the offer and is now waiting for answer
     */
    WAITING_FOR_ANSWER,
    /**
     * Waiting for establishing the connection.
     */
    WAITING_TO_CONNECT,
    /**
     * Connection was established. You can chat now.
     */
    CHAT_ESTABLISHED,
    /**
     * Connection is terminated chat ended.
     */
    CHAT_ENDED
}