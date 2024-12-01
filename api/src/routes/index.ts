import { Router, Request, Response } from 'express'

import notificationsRoutes from './notifications'
import { ROUTES } from '../utils/constants'
import { LandingController } from '../controllers/landing'

const mainRouter = Router()
const landingController = new LandingController()

mainRouter.get(ROUTES.ROOT, (req: Request, res: Response) => {
    res.sendFile(landingController.renderHomePage())
})

// Use the web routes with the base path /web
mainRouter.use(ROUTES.NOTIFICATIONS, notificationsRoutes)

export default mainRouter
