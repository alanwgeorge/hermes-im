# hermes-im
![logo](app/src/main/res/drawable-xhdpi/ic_launcher.png?raw=true  "Logo")


Secure messaging app for Android.  Uses Google Cloud Messaging (GCM) as device to device transport (no backend required, other than GCM, of course).

Work in progress, currently can register new contacts with QR code scan and can send and receive messages. Need to add to conversation and contact management.  See other TODOs below. 

## Tech Employed
* [AndroidKeyStore](https://developer.android.com/training/articles/keystore.html): To store the user's [KeyPair](http://developer.android.com/reference/java/security/KeyPair.html)
* [ZXing](https://github.com/zxing/zxing/): Coding and decoding QR codes into contacts for exchanging contact addresses
* gcm-server.jar: used to send push messages device to device
* [ContentProvider](http://developer.android.com/guide/topics/providers/content-providers.html)/[SQLite](http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html): persist contacts and messages, messages stored encrypted
* Symmetric Encryption (AES): used encrypt/decrypt message text
* Asymmetric Encryption (RSA): used to encrypt/decrypt symmetric key with public/private key pair
* Message Signature (SHA512withRSA): ensure message originated from specific key pair
 
## Overview of Message Send Sequence
![Overview of Message Send Sequence](docs/Hermes-Overview-Message-Send.png?raw=true  "Overview")

## Screenshot
![Screenshot conversation](screenshots/Conversation.png?raw=true  "Conversation")
![Screenshot sign on](screenshots/SignOn.png?raw=true  "Sign on")

## TODO
* Security review
* Delete contact(s)
* Unread messages on contact list not reading data store
* Conversation UI need a design
*	Optionally add Contact when receiving message for unknown contact
*	Explore other ways to exchange public keys and GCM IDs current can only exchange via QR code
*	Explore other ways to transport messages, currently uses Google Cloud Messaging (GCM)
*	New logo, current one doesn't play well on dark wallpapers
