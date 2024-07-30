export const CONSTANTS = {
    SERVER_PORT: 4444,

    LOGS: {
        ALL_FILE: 'logs/all.log',
        ERROR_FILE: 'logs/error.log',
    },

    ROUTE_PREFIX: '/api/v1',
}

export const ROUTES = {
    ROOT: CONSTANTS.ROUTE_PREFIX + '/',
    LAB: CONSTANTS.ROUTE_PREFIX + '/lab',
}
