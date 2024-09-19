import { Request, Response } from 'express'
import express from 'express'
import fs from 'fs'
import path from 'path'

import { handleJsonPost } from '../controllers'
import firebaseAdmin from '../config/firebase'
import { authenticateApiKey } from '../middleware/apiKeyMiddleware'
import { NOTIFICATION_TYPE } from '../utils/enums'

const router = express.Router()

router.use(authenticateApiKey)

router.get('/', (req: Request, res: Response) => {
    res.status(200).json({ message: 'Hello from Lab Router!' })
})

router.get('/alert/:id', (req: Request, res: Response) => {
    const { id } = req.params
    const idNumber: number = Number(id)

    let message
    switch (idNumber) {
        case 0:
            message = {
                topic: 'debug',
                data: {
                    title: 'Mushu Debug Alert',
                    body: 'The fish are dying...maybe? ',
                    type: NOTIFICATION_TYPE.ALERT
                },
                android: {
                    priority: "high" as "high",
                },
            }
            break
        case 1:
            message = {
                data: {
                    title: 'Mushu Alert',
                    body: 'The fish are dying! You monster!',
                    type: NOTIFICATION_TYPE.ALERT
                },
                android: {
                    priority: "high" as "high",
                },
                topic: 'allUsers',
            }
            break
        default:
            message = {
                data: {
                    title: 'Mushu Alert Default',
                    body: 'Default message',
                    type: NOTIFICATION_TYPE.ALERT
                },
                android: {
                    priority: "high" as "high",
                },
                topic: 'allUsers',
            }
            break
    }

    console.log('Sending alert: ', message.data.title)

    // Send the message using the Admin SDK
    firebaseAdmin
        .messaging()
        .send(message)
        .then(response => {
            console.log('Successfully sent message:', response)
            res.status(200).send('Notification sent successfully')
        })
        .catch(error => {
            console.log('Error sending message:', error)
            res.status(500).send('Error sending notification')
        })
})

router.get('/notify/:id', (req: Request, res: Response) => {
    const { id } = req.params
    const idNumber: number = Number(id)

    let message
    switch (idNumber) {
        case 0:
            message = {
                data: {
                    title: 'Mushu Debug Notification',
                    body: 'The fish are dying...maybe? ',
                    type: NOTIFICATION_TYPE.NOTIFIER
                },
                topic: 'debug',
            }
            break
        case 1:
            message = {
                data: {
                    title: 'Mushu Notification',
                    body: 'The fish are dying! You monster!',
                    type: NOTIFICATION_TYPE.NOTIFIER
                },
                topic: 'allUsers',
            }
            break
        default:
            message = {
                data: {
                    title: 'Mushu Notification Default',
                    body: 'Default notifier message',
                    type: NOTIFICATION_TYPE.NOTIFIER
                },
                topic: 'allUsers',
            }
            break
    }

    console.log('Sending notification: ', message.data.title)

    // Send the message using the Admin SDK
    firebaseAdmin
        .messaging()
        .send(message)
        .then(response => {
            console.log('Successfully sent message:', response)
            res.status(200).send('Notification sent successfully')
        })
        .catch(error => {
            console.log('Error sending message:', error)
            res.status(500).send('Error sending notification')
        })
})


router.post('/', (req: Request, res: Response) => {
    console.log(req.body.coiso)
    const id = req.body.id
    const date = new Date()
    const formattedTimestamp = `${date.getFullYear()}${(date.getMonth() + 1).toString().padStart(2, '0')}${date.getDate().toString().padStart(2, '0')}-${date.getHours().toString().padStart(2, '0')}${date.getMinutes().toString().padStart(2, '0')}${date.getSeconds().toString().padStart(2, '0')}`

    if (!id) {
        return res.status(400).json({ message: 'ID is required in the JSON data' })
    }

    const filename = `${id}_${formattedTimestamp}.json`.replace(/:/g, '-')
    const filePath = path.join(__dirname, '..', '..', 'files', filename)

    // Save the JSON data to a file
    fs.writeFile(filePath, JSON.stringify(req.body, null, 2), err => {
        if (err) {
            console.error('Error saving the file:', err)
            return res.status(500).json({ message: 'Failed to save the file' })
        }

        res.status(200).json({ message: 'File saved successfully', filename })
    })
})

export default router
