## Firebase Admin SDK access

For using the Firebase Cloud Messaging service, the Firebase SDK needs a private key to work. To generate your own key follow these steps:

1. Go to the Firebase Console and go to the `Project Settings`
2. Go to the `Service accounts` tab
3. Click the `Generate new private key` button. It will start downloading a JSON file with the key
4. Rename the file to `firebase-service-account.json`
5. Place this file under `api/config/firebase-service-account.json`
