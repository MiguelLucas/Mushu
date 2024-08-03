import { Router, Request, Response } from 'express'

import labRoutes from './lab'
import { authenticateApiKey } from '../middleware/apiKeyMiddleware'
import { ROUTES } from '../utils/constants'
import { LandingController } from '../controllers/landing'

const mainRouter = Router()
const landingController = new LandingController()

//mainRouter.use(authenticateApiKey)

mainRouter.get(ROUTES.ROOT, (req: Request, res: Response) => {
    res.sendFile(landingController.renderHomePage());
    //res.sendFile(path.join(__dirname, '../views/landing.html'));

})

// Use the web routes with the base path /web
mainRouter.use(ROUTES.LAB, labRoutes)

export default mainRouter
