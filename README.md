# DriveHostingAndroid
A demonstration of Google Drive capability to host files for Android applications

## Problem
The considering problem is to utilize a Google Drive account to act as a host server, to create N folders and grant permission for N accounts to access these folders. Each client, aka an Android application, will upload its data to its corresponding folder. Strictly speaking, each application must have the ability to perform read and write operations on that folder.

## Approaches
There are indeed 2 distinct libraries which support operations with Google Drive.
### [Google Drive Android API](https://developers.google.com/drive/android/) (GDAA)
This approach is simple and straightforward to implement. It is, in fact, integrated in Google Play Services, so it should be the same as other Google API libraries. According to the documentation, Android developers should use this in the majority of cases to gain the best performance (and easy implementation).  
A considerable drawback is the fact that it only supports limited file scope, in other words an application can only access those files created by itself.
### [Google Drive REST API](https://developers.google.com/drive/v3/web/quickstart/android)
This method is actually far more complicated than the previous method, in consequence of its convoluted setup and not-very-detailed documentation. As a result, documentation states that you should thoroughly review the GDAA and use it if possible before actually using the REST API.  
Notwithstanding the troublesome implementation, REST API do have a handful of advanced and useful capabilities, especially those not supported by GDAA. The file access permission, for instance, can be granted with full drive scope, gaining access to files outside of current application.

## Demo
The demo consists of a list showing the full list of shared-with-me folders and files and a button to add one more file to the shared folder. Keep in mind that the shared folder should be named the same as the given email.  
This application is only built as a Proof of Concept, not a robust software ready for production. Please don't try to break it (awfully easy to do that).

## Conclusion
Although it wholly depends on your project and there is no definite answer to choose which approach to use, GDAA should be  considered first, and ought to be used as long as it suits your needs. On the other hand, if its requirements exceed beyond GDAA capabilities, Google Drive REST API should be a great choice. 
