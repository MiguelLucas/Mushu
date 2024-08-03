import { Request, Response } from 'express'
import express from 'express'
import fs from 'fs'
import path from 'path'

import { handleJsonPost } from '../controllers'
import firebaseAdmin from '../config/firebase'

const router = express.Router()

router.get('/', (req: Request, res: Response) => {
    res.status(200).json({ message: 'Hello from Lab Router!' })
})

router.get('/notify/:id', (req: Request, res: Response) => {
    const { id } = req.params
    const idNumber: number = Number(id)

    let message
    switch (idNumber) {
        case 1:
            message = {
                notification: {
                    title: 'Mushu Alert',
                    body: 'The fish are dying! You monster!',
                },
                topic: 'allUsers',
            }
            break
        default:
            message = {
                notification: {
                    title: 'Mushu Alert 2',
                    body: 'Some other alert can be sent',
                },
                topic: 'allUsers',
            }
            break
    }

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

router.get('/lab/:id', (req: Request, res: Response) => {
    const { id } = req.params
    res.send(`Evaluating ID: ${id}`)
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
