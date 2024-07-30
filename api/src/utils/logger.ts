import winston from 'winston'
import { CONSTANTS } from './constants'

const logger = winston.createLogger({
    level: 'debug',
    transports: [
        new winston.transports.Console({
            format: winston.format.combine(winston.format.colorize({ all: true })),
        }),

        new winston.transports.File({ filename: CONSTANTS.LOGS.ALL_FILE, options: { flags: 'w' } }),
        new winston.transports.File({ filename: CONSTANTS.LOGS.ERROR_FILE, level: 'error', options: { flags: 'w' } }),
    ],

    format: winston.format.combine(
        winston.format.timestamp({
            format: 'YYYY-MM-DD HH:mm:ss',
        }),
        winston.format.printf(info => `[${info.timestamp}][${info.level.toUpperCase()}] ${info.message}`),
    ),
})

export { logger }
