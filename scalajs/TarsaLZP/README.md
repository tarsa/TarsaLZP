### Running

This TarsaLZP implementation uses https://github.com/lihaoyi/workbench SBT 
plugin.

To run development version run: ```sbt ~fastOptJS``` then go to
http://localhost:12345/target/scala-2.11/classes/index-dev.html in the browser.

To run production version run: ```sbt ~fullOptJS``` then go to
http://localhost:12345/target/scala-2.11/classes/index.html in the browser.

Development version has workbench.js script plugged in so the application will
refresh automatically in the browser window as code is changed and recompiled.
Production version requires manual refreshing in the browser.