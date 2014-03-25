The source folder contains source code for Android, Pebble, and Web
Code has additionally been compiled for easy installation

----------------------------------------------------------------
The Android 
----------------------------------------------------------------

Current limitations:
	Address-to-coordinate conversion is based on Google Services. Addresses are not verified for correctness.
	The App disables landscape mode while in use.

To install the Android application
    1. Plug in your phone, and drag the compiled "Capsrock.apk" file into the downloads folder
    2. Open the downloads folder on the phone and click on the icon that says "Capsrock"

There is no login screen to the application. A template is set up that, when enabled via uncommenting its Intent call, will allow access to the app with following credentials ONLY:
    username: admin@capsrock.com
    password: 42513

----------------------------------------------------------------
The Pebble
----------------------------------------------------------------
Known Bugs:
    If you exit the Pebble application while logging time at a location and then return to the Pebble application, the time will properly update but the Pebble will display "Not on Location"
    
    1. Download the "Pebble" application from the Android Play store
    2. Open the application and go to settings. Select "Install Untrusted Apps" and "Developer Options->Enable Developer Connection"
    3. Plug in your phone, and drag the compiled "Capsrock.pbw" into the downloads folder
    4. Open the downloads folder on the phone and click on the icon that says "Capsrock"


----------------------------------------------------------------
Web Service
----------------------------------------------------------------
    1. Install django on your machine.
    2. Install django rest framework; instructions at http://www.django-rest-framework.org/#installation
    3. Navigate to /Source/web and run "python manage.py runserver 0.0.0.0:8000"
    4. Obtain your IP address (On mac you can get your IP address by running "ifconfig" in terminal, the number you want is under en1: inet)
    5. Launch the Android application (This builds the folder and file which contain server info)
    6. Exit the application and click on MyFiles, then the Capsrock folder, and then serverInfo.txt
    7. Replace the generic IP address in that file with the IP address obtained in step 4.
    8. Unless your computer has a globally available IP, make sure that phone is connected to the same network as the computer.

You can view the database using any sqlite3 tool, or from django's handy admin page
The admin page is at "http://127.0.0.1:8000/admin"
Admin account for the django database
    username: admin
    password: 42513