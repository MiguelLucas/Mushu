import { NOTIFICATION_TYPE } from '../utils/enums'

export interface FirebaseMessageResult {
    success: boolean
    data?: string
    error?: string
}

export interface FirebaseMessage {
    data: {
        title: string
        body: string
        type: NOTIFICATION_TYPE
    },
    topic: string

}