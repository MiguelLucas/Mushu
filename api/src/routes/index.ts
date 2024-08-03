import { Router, Request, Response } from 'express'

import labRoutes from './lab'
import { ROUTES } from '../utils/constants'
import { LandingController } from '../controllers/landing'

const mainRouter = Router()
const landingController = new LandingController()

mainRouter.get(ROUTES.ROOT, (req: Request, res: Response) => {
    res.sendFile(landingController.renderHomePage())
})

// Use the web routes with the base path /web
mainRouter.use(ROUTES.LAB, labRoutes)

export default mainRouter
