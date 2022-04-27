# Finder

The goal of this project is to help you recover devices connected to your local network. This project is still experimental, if you want to make your contribution, you are welcome!

## DISCLAIMER: 
I am in no way responsible if you use the application for malicious purposes (tracking, etc...) please use this project with kindness!

## Current compatibility:
This project is only compatible with Freeboxes, but I'm working on a larger compatibility.

## Before using the app, please change the following lines in ./app/src/main/java/com/anynone/finderMainActivity.java :

line 95 : ```private String app_token="yourapptoken";```  get an application token and replace the value in quotes by the token you get (more informations here : https://dev.freebox.fr/sdk/os/login/#)

line 98 : ```private String url = "https://urltoyourfreebox:portnumber/";```  replace the value in the quotes by your freebox url.


In a future release, theses values will be changed programmatically.

I hope you will like it!
