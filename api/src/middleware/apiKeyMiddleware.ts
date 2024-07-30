import { Request, Response, NextFunction } from 'express'
import { Config } from '../../config/config'

const mushu_header = 'X-Mushu-Key'

export function authenticateApiKey(req: Request, res: Response, next: NextFunction) {
    const apiKey = req.header(mushu_header) // The header field to check for the API key
    const validApiKey = Config.API_KEY

    if (apiKey === validApiKey) {
        // API key is valid, proceed to the next middleware or route handler
        next()
    } else {
        // API key is invalid, reject the request
        res.status(403).send('Unauthorized')
    }
}
