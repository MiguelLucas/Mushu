import { Request, Response } from 'express'
import asyncHandler from 'express-async-handler'
import { isEmpty } from 'lodash'

import { NOTIFICATION_TYPE } from '../utils/enums'
import { sendFirebaseMessage } from '../services/firebase'
import { FirebaseMessageResult, FirebaseMessage } from '../types/firebase'

const TAG = '[NotificationsController]'

const validateNotificationRequest = (reqBody: any) => {
    if (isEmpty(reqBody)) {
        console.error(`${TAG} Request body not present/empty`)
        return false
    }

    return true
}

const handleNotificationRequest = async (req: Request, res: Response, type: NOTIFICATION_TYPE) => {
    const reqBody = req.body

    if (!validateNotificationRequest(reqBody)) {
        res.status(400).json({
            message: `No request body!`,
        })
        return
    }

    let message: FirebaseMessage = {
        data: {
            title: reqBody.title || 'Mushu Debug Notification',
            body: reqBody.body || 'Please ignore if you are getting this notification',
            type: type,
        },
        topic: reqBody.topic || 'debug',
    }

    try {
        console.log(`${TAG} Sending ${type}: ${message.data.title}`)
        let firebaseRes: FirebaseMessageResult = await sendFirebaseMessage(message)
        if (firebaseRes.success) {
            res.status(204).send()
        } else {
            res.status(500).send(firebaseRes.error)
        }
    } catch (error) {
        res.status(500).send('Error using Firebase Cloud Messaging service')
    }
}

export const handleNotify = asyncHandler(async (req: Request, res: Response) => {
    console.log(`${TAG} Handling notification request`)
    handleNotificationRequest(req, res, NOTIFICATION_TYPE.NOTIFIER)
})

export const handleAlert = asyncHandler(async (req: Request, res: Response) => {
    console.log(`${TAG} Handling alert request`)
    handleNotificationRequest(req, res, NOTIFICATION_TYPE.ALERT)
})
