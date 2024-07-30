import { Router, Request, Response } from 'express'

import labRoutes from './lab'

import { ROUTES } from '../utils/constants'

const mainRouter = Router()

mainRouter.get(ROUTES.ROOT, (req: Request, res: Response) => {
    res.send(`Hello from Main Router!`)
})

// Use the web routes with the base path /web
mainRouter.use(ROUTES.LAB, labRoutes)

export default mainRouter
