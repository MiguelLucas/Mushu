import firebaseAdmin from '../config/firebase'
import { FirebaseMessageResult, FirebaseMessage } from '../types/firebase'

const TAG = '[FirebaseService]'

export const sendFirebaseMessage = async (message: FirebaseMessage): Promise<FirebaseMessageResult> => {
    try {
        const response = await firebaseAdmin.messaging().send(message)
        console.log(`${TAG} Successfully sent message:`, response)
        console.log(`${TAG} Title:`, message.data.title)
        console.log(`${TAG} Body:`, message.data.body)

        return { success: true }
    } catch (error) {
        console.log(`${TAG} Error sending message:`, error)
        return { success: false, error: 'Error sending notification' }
    }
}
