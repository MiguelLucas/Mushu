import { expect } from '@jest/globals'
import request from 'supertest'
import app from '../../src/app'
import { sendFirebaseMessage } from '../../src/services/firebase'
import { NOTIFICATION_TYPE } from '../../src/utils/enums'

// Mock the Firebase Service to manipulate its responses
jest.mock('../../src/services/firebase', () => ({
    sendFirebaseMessage: jest.fn(),
}))

// Mock the authentication middleware so to let pass every request
jest.mock('../../src/middleware/apiKeyMiddleware', () => ({
    authenticateApiKey: (req: any, res: any, next: any) => next(),
}))

// Mock console logs and do not print them
const logSpy = jest.spyOn(console, 'log').mockImplementation(() => {})
const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => {})

const mockSendFirebaseMessage = sendFirebaseMessage as jest.MockedFunction<typeof sendFirebaseMessage>

describe('Notifications Controller', () => {
    let testApp: request.Agent

    beforeEach(() => {
        jest.clearAllMocks()
        testApp = request(app)
    })

    describe('POST /notify', () => {
        const endpoint = '/api/v1/notifications/notify'

        it('Should return 400 when request body is empty', async () => {
            const response = await testApp.post(endpoint).send()

            expect(response.status).toBe(400)
            expect(response.body.message).toBe('No request body!')
            expect(mockSendFirebaseMessage).not.toHaveBeenCalled()
        })

        it('Should send notification and return 204 on success', async () => {
            (sendFirebaseMessage as jest.MockedFunction<typeof sendFirebaseMessage>).mockResolvedValue({
                success: true,
            })

            const payload = {
                title: 'Test Notification',
                body: 'Test Body',
                topic: 'test-topic',
            }

            const response = await testApp.post(endpoint).send(payload)

            expect(response.status).toBe(204)
            expect(mockSendFirebaseMessage).toHaveBeenCalledWith({
                data: {
                    title: payload.title,
                    body: payload.body,
                    type: NOTIFICATION_TYPE.NOTIFIER,
                },
                topic: payload.topic,
            })
        })

        it('Should return 500 when Firebase Cloud Messaging service fails', async () => {
            (sendFirebaseMessage as jest.MockedFunction<typeof sendFirebaseMessage>).mockResolvedValue({
                success: false,
                error: 'Error sending notification',
            })
            const payload = {
                title: 'Test Notification',
                body: 'Test Body',
                topic: 'test-topic',
            }

            const response = await testApp.post(endpoint).send(payload)

            expect(response.status).toBe(500)
            expect(response.text).toBe('Error sending notification')
            expect(mockSendFirebaseMessage).toHaveBeenCalled()
        })
    })

    describe('POST /alert', () => {
        const endpoint = '/api/v1/notifications/alert'

        it('Should return 400 when request body is empty', async () => {
            const response = await testApp.post(endpoint).send()

            expect(response.status).toBe(400)
            expect(response.body.message).toBe('No request body!')
            expect(mockSendFirebaseMessage).not.toHaveBeenCalled()
        })

        it('Should send notification and return 204 on success', async () => {
            (sendFirebaseMessage as jest.MockedFunction<typeof sendFirebaseMessage>).mockResolvedValue({
                success: true,
            })

            const payload = {
                title: 'Test Notification',
                body: 'Test Body',
                topic: 'test-topic',
            }

            const response = await testApp.post(endpoint).send(payload)

            expect(response.status).toBe(204)
            expect(mockSendFirebaseMessage).toHaveBeenCalledWith({
                data: {
                    title: payload.title,
                    body: payload.body,
                    type: NOTIFICATION_TYPE.ALERT,
                },
                topic: payload.topic,
            })
        })

        it('Should return 500 when Firebase Cloud Messaging service fails', async () => {
            (sendFirebaseMessage as jest.MockedFunction<typeof sendFirebaseMessage>).mockResolvedValue({
                success: false,
                error: 'Error sending notification',
            })
            const payload = {
                title: 'Test Notification',
                body: 'Test Body',
                topic: 'test-topic',
            }

            const response = await testApp.post(endpoint).send(payload)

            expect(response.status).toBe(500)
            expect(response.text).toBe('Error sending notification')
            expect(mockSendFirebaseMessage).toHaveBeenCalled()
        })
    })
})
