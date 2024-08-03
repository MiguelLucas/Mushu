import express, { Application } from 'express'
import path from 'path'

import mainRouter from './routes/index'
import { logger } from './utils/logger'
import { CONSTANTS } from './utils/constants'

const app: Application = express()
const port = CONSTANTS.SERVER_PORT

// Middleware to parse JSON bodies
app.use(express.json())
// and to support URL-encoded bodies
app.use(express.urlencoded({ extended: true }))
app.use('/assets', express.static(path.join(__dirname, 'assets')))

// Use the consolidated routes
app.use(mainRouter)

app.listen(port, () => {
    logger.warn(`Mushu API listening at http://localhost:${CONSTANTS.SERVER_PORT}`)
})

export default app
