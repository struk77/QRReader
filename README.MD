QR-code reader for Android 7+ based on QReader https://github.com/nisrulz/qreader

### Specs
[![API](https://img.shields.io/badge/API-24%2B-orange.svg?style=flat)](https://android-arsenal.com/api?level=24)

> Requires Google Play Services

# Usage
This application scans QR-code and sends it to server by POST request with json payloads, specified in settings. 
There are four settings: url, login, password and list_id.
Response from server must be in expected format:
```json
{"status":"ok", "description":"Data successfully added to list <b>Goods</b>"}
```
or
```json
{"status":"error", "description":"Code already exists in list <b>Grocery</b>"}
```

Description field supports some html tags for text formatting (bold, cursive etc)
