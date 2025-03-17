package model

enum class ErrorType {
    BAD_REQUEST,
    NOT_FOUND,
    UNAUTHORIZED,
    FORBIDDEN,
    INTERNAL_SERVER_ERROR,
    NETWORK_ERROR,
    UNKNOWN_ERROR,
}