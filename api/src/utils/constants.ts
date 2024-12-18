export const CONSTANTS = {
    SERVER_PORT: 4444,

    LOGS: {
        ALL_FILE: 'logs/all.log',
        ERROR_FILE: 'logs/error.log',
    },

    API_PREFIX: '/api/v1',
}

export const ROUTES = {
    ROOT: '/',
    NOTIFICATIONS: CONSTANTS.API_PREFIX + '/notifications',
}

export const NOTIFICATIONS_ROUTES = {
    ROOT: '/',
    NOTIFY: '/notify',
    ALERT: '/alert',
}
